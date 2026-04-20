package com.mahub.parser;

import com.mahub.parser.model.*;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.*;

/**
 * Parses interface definition Markdown into a {@link ServiceSpec} IR.
 * Thread-safe: the static flexmark {@link Parser} is immutable after construction.
 * Returns an empty/default ServiceSpec on null or blank input — callers should
 * run the result through {@link ServiceSpecValidator} to surface structural issues.
 */
public class MarkdownServiceParser {

    private static final Logger LOG = Logger.getLogger(MarkdownServiceParser.class.getName());
    private static final Parser PARSER = Parser.builder(new MutableDataSet()).build();

    // Matches: fieldName: TypeName[<Generic>] [constraint1, constraint2]
    private static final Pattern FIELD_PATTERN =
            Pattern.compile("^(\\w[\\w-]*):\\s*(\\w+(?:<[^>]+>)?)(?:\\s*\\[([^]]+)])?");

    public ServiceSpec parse(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return new ServiceSpec("", "", List.of(), List.of(),
                    List.of(), List.of(), List.of(), "rest", "postgresql", 8080, "/");
        }

        Document doc = PARSER.parse(markdown);
        List<Node> nodes = collectNodes(doc);

        String serviceName = "";
        String description = "";
        List<DomainModel> domainModels = new ArrayList<>();
        List<ApiEndpoint> apiEndpoints = new ArrayList<>();
        List<DependencyRef> dependencies = new ArrayList<>();
        List<EventSpec> publishedEvents = new ArrayList<>();
        List<EventSpec> consumedEvents = new ArrayList<>();
        String communication = "rest";
        String dbType = "postgresql";
        int port = 8080;
        String contextPath = "/";

        String currentSection = "";
        String currentSubSection = "";

        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);

            if (node instanceof Heading heading) {
                String text = heading.getText().toString().trim();
                int level = heading.getLevel();

                if (level == 1 && text.startsWith("Service:")) {
                    serviceName = text.substring("Service:".length()).trim();
                    currentSection = "";
                } else if (level == 2) {
                    currentSection = text;
                    currentSubSection = "";
                } else if (level == 3) {
                    currentSubSection = text;

                    if (currentSection.equals("Domain Models")) {
                        domainModels.add(parseDomainModel(currentSubSection, collectBulletItems(nodes, i + 1)));
                    } else if (currentSection.equals("APIs")) {
                        apiEndpoints.add(parseApiEndpoint(currentSubSection, collectBulletItems(nodes, i + 1)));
                    } else if (currentSection.equals("Dependencies")) {
                        dependencies.addAll(parseDependencies(currentSubSection, collectBulletItems(nodes, i + 1)));
                    }
                }
            } else if (node instanceof Paragraph para) {
                String text = para.getContentChars().toString().trim();
                if (currentSection.equals("Description") && !text.isEmpty()) {
                    description = text;
                }
            }
        }

        // Second pass: Events and Communication/Config use flat bullet lists under H2
        currentSection = "";
        currentSubSection = "";
        for (Node node : nodes) {
            if (node instanceof Heading h) {
                String text = h.getText().toString().trim();
                if (h.getLevel() == 2) {
                    currentSection = text;
                    currentSubSection = "";
                } else if (h.getLevel() == 3) {
                    currentSubSection = text;
                }
            } else if (node instanceof BulletList bl) {
                List<String> items = extractBulletItems(bl);
                if (currentSection.equals("Events")) {
                    if (currentSubSection.equals("Published")) {
                        publishedEvents.addAll(parseEventSpecs(items));
                    } else if (currentSubSection.equals("Consumed")) {
                        consumedEvents.addAll(parseEventSpecs(items));
                    }
                } else if (currentSection.equals("Communication")) {
                    for (String item : items) {
                        String[] parts = splitKeyValue(item);
                        if (parts[0].equalsIgnoreCase("Outbound")) communication = parts[1];
                        else if (parts[0].equalsIgnoreCase("DB")) dbType = parts[1];
                    }
                } else if (currentSection.equals("Config")) {
                    for (String item : items) {
                        String[] parts = splitKeyValue(item);
                        if (parts[0].equalsIgnoreCase("port")) {
                            try {
                                port = Integer.parseInt(parts[1]);
                            } catch (NumberFormatException e) {
                                LOG.warning("## Config: invalid port value '" + parts[1] + "', using default 8080");
                            }
                        } else if (parts[0].equalsIgnoreCase("context-path")) {
                            contextPath = parts[1];
                        }
                    }
                }
            }
        }

        return new ServiceSpec(serviceName, description, domainModels, apiEndpoints,
                dependencies, publishedEvents, consumedEvents, communication, dbType, port, contextPath);
    }

    private List<Node> collectNodes(Document doc) {
        List<Node> result = new ArrayList<>();
        Node child = doc.getFirstChild();
        while (child != null) {
            result.add(child);
            child = child.getNext();
        }
        return result;
    }

    /**
     * Scans forward from startIndex until the first BulletList or the next Heading.
     * Does not cap at a hard-coded lookahead — stops naturally at section boundaries.
     */
    private List<String> collectBulletItems(List<Node> nodes, int startIndex) {
        for (int i = startIndex; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            if (n instanceof BulletList bl) return extractBulletItems(bl);
            if (n instanceof Heading) break;
        }
        return List.of();
    }

    private List<String> extractBulletItems(BulletList bl) {
        List<String> items = new ArrayList<>();
        Node item = bl.getFirstChild();
        while (item != null) {
            if (item instanceof BulletListItem li) {
                Node content = li.getFirstChild();
                if (content instanceof Paragraph p) {
                    items.add(p.getContentChars().toString().trim());
                }
            }
            item = item.getNext();
        }
        return items;
    }

    private DomainModel parseDomainModel(String entityName, List<String> items) {
        List<FieldSpec> fields = new ArrayList<>();
        for (String item : items) {
            Matcher m = FIELD_PATTERN.matcher(item);
            if (m.find()) {
                String name = m.group(1);
                String type = m.group(2);
                List<String> constraints = m.group(3) != null
                        ? Arrays.asList(m.group(3).split(",\\s*"))
                        : List.of();
                fields.add(new FieldSpec(name, type, constraints));
            }
        }
        return new DomainModel(entityName, fields);
    }

    private ApiEndpoint parseApiEndpoint(String header, List<String> items) {
        String[] parts = header.split("\\s+", 2);
        String method = parts.length > 0 ? parts[0].toUpperCase() : "";
        String path = parts.length > 1 ? parts[1] : "";

        String summary = "";
        String auth = "none";
        DtoSpec requestDto = null;
        DtoSpec responseDto = null;
        List<Integer> errors = new ArrayList<>();

        for (String item : items) {
            String[] kv = splitKeyValue(item);
            switch (kv[0].toLowerCase()) {
                case "summary" -> summary = kv[1];
                case "auth" -> auth = kv[1];
                case "request" -> requestDto = parseDtoSpec(kv[1]);
                case "response" -> responseDto = parseDtoSpec(kv[1]);
                case "errors" -> {
                    for (String code : kv[1].split("[|,]\\s*")) {
                        String trimmed = code.trim();
                        if (!trimmed.isEmpty()) {
                            try {
                                errors.add(Integer.parseInt(trimmed));
                            } catch (NumberFormatException e) {
                                LOG.warning("API " + path + ": unrecognized error code '" + trimmed + "'");
                            }
                        }
                    }
                }
            }
        }
        return new ApiEndpoint(method, path, summary, auth, requestDto, responseDto, errors);
    }

    private DtoSpec parseDtoSpec(String raw) {
        Pattern p = Pattern.compile("^(\\w+)(?:\\s*\\{([^}]+)})?");
        Matcher m = p.matcher(raw.trim());
        if (!m.find()) return new DtoSpec(raw.trim(), List.of());

        String name = m.group(1);
        List<FieldSpec> fields = new ArrayList<>();
        if (m.group(2) != null) {
            for (String fieldStr : m.group(2).split(",\\s*(?=\\w+:)")) {
                String[] fParts = fieldStr.trim().split(":\\s*", 2);
                if (fParts.length == 2) {
                    fields.add(new FieldSpec(fParts[0].trim(), fParts[1].trim(), List.of()));
                }
            }
        }
        return new DtoSpec(name, fields);
    }

    private List<DependencyRef> parseDependencies(String serviceName, List<String> items) {
        List<DependencyRef> refs = new ArrayList<>();
        for (String item : items) {
            int colonIdx = item.indexOf(':');
            if (colonIdx > 0) {
                String endpoint = item.substring(0, colonIdx).trim();
                String desc = item.substring(colonIdx + 1).trim();
                String[] ep = endpoint.split("\\s+", 2);
                String method = ep.length > 0 ? ep[0].toUpperCase() : "";
                String path = ep.length > 1 ? ep[1] : "";
                refs.add(new DependencyRef(serviceName, method, path, desc));
            }
        }
        return refs;
    }

    private List<EventSpec> parseEventSpecs(List<String> items) {
        List<EventSpec> events = new ArrayList<>();
        for (String item : items) {
            if (item.equalsIgnoreCase("(none)") || item.equalsIgnoreCase("none")) continue;
            int colonIdx = item.indexOf(':');
            if (colonIdx > 0) {
                events.add(new EventSpec(item.substring(0, colonIdx).trim(), item.substring(colonIdx + 1).trim()));
            } else {
                events.add(new EventSpec(item.trim(), ""));
            }
        }
        return events;
    }

    private String[] splitKeyValue(String item) {
        int idx = item.indexOf(':');
        if (idx < 0) return new String[]{item.trim(), ""};
        return new String[]{item.substring(0, idx).trim(), item.substring(idx + 1).trim()};
    }
}
