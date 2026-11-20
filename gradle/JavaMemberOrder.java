import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/** Checks source declaration order without compiling or resolving dependencies. */
public final class JavaMemberOrder {
    private enum Group {
        STATIC_FINAL_FIELD("static final field"),
        STATIC_FIELD("static field"),
        FINAL_FIELD("final instance field"),
        FIELD("instance field"),
        STATIC_TYPE("static nested type"),
        INNER_TYPE("inner class"),
        STATIC_METHOD("static method"),
        CONSTRUCTOR("constructor"),
        METHOD("instance method");

        private final String label;

        Group(String label) {
            this.label = label;
        }
    }

    private record Member(Group group, String name) {}

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            throw new IllegalArgumentException("Supply at least one Java source directory.");
        }
        List<Path> sources;
        try (var paths = Stream.of(args).flatMap(JavaMemberOrder::javaFiles)) {
            sources = paths.distinct().sorted().toList();
        }
        if (!sources.isEmpty() && !checkOrder(sources)) {
            System.exit(1);
        }
    }

    private static boolean checkOrder(List<Path> sources) throws IOException {
        Path root = Path.of("").toAbsolutePath().normalize();
        var compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            var task = (JavacTask) compiler.getTask(
                    null,
                    files,
                    diagnostics,
                    List.of("-proc:none", "--release", "21"),
                    null,
                    files.getJavaFileObjectsFromPaths(sources));
            var positions = Trees.instance(task).getSourcePositions();
            var violations = new ArrayList<String>();
            var checker = new TreeScanner<Void, CompilationUnitTree>() {
                @Override
                public Void visitClass(ClassTree type, CompilationUnitTree unit) {
                    String file = root.relativize(Path.of(unit.getSourceFile().toUri()))
                            .toString()
                            .replace('\\', '/');
                    Member latest = null;
                    for (var member : type.getMembers()) {
                        Group group = group(type, member);
                        if (member instanceof VariableTree field
                                && positions.getEndPosition(unit, field.getType()) == Diagnostic.NOPOS) {
                            // Enum constants have a synthetic type tree.
                            continue;
                        }
                        if (group == null) {
                            continue;
                        }
                        if (latest != null && group.compareTo(latest.group()) < 0) {
                            long offset = positions.getStartPosition(unit, member);
                            violations.add("%s:%d: %s '%s' must appear before %s '%s'"
                                    .formatted(
                                            file,
                                            unit.getLineMap().getLineNumber(offset),
                                            group.label,
                                            name(member),
                                            latest.group().label,
                                            latest.name()));
                        } else {
                            latest = new Member(group, name(member));
                        }
                    }
                    return super.visitClass(type, unit);
                }
            };
            for (var unit : task.parse()) {
                checker.scan(unit, unit);
            }
            var errors = diagnostics.getDiagnostics().stream()
                    .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                    .toList();
            if (!errors.isEmpty()) {
                errors.forEach(System.err::println);
                throw new IllegalStateException("Cannot check Java member order with syntax errors.");
            }
            violations.forEach(System.err::println);
            return violations.isEmpty();
        }
    }

    private static String name(Tree member) {
        return switch (member) {
            case ClassTree type ->
                type.getSimpleName().isEmpty()
                        ? "<anonymous>"
                        : type.getSimpleName().toString();
            case VariableTree field -> field.getName().toString();
            case MethodTree method ->
                method.getName()
                        + method.getParameters().stream()
                                .map(parameter -> parameter.getType().toString().replaceAll("\\s+", ""))
                                .collect(Collectors.joining(",", "(", ")"));
            default -> throw new IllegalArgumentException("Unexpected member: " + member.getKind());
        };
    }

    private static Stream<Path> javaFiles(String directory) {
        Path root = Path.of(directory);
        if (!Files.isDirectory(root)) {
            return Stream.empty();
        }
        try (var paths = Files.walk(root)) {
            return paths
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .toList()
                    .stream();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static Group group(ClassTree owner, Tree member) {
        boolean interfaceBody = owner.getKind() == Tree.Kind.INTERFACE || owner.getKind() == Tree.Kind.ANNOTATION_TYPE;
        return switch (member) {
            case VariableTree field -> {
                var flags = field.getModifiers().getFlags();
                boolean isStatic = interfaceBody || flags.contains(Modifier.STATIC);
                boolean isFinal = interfaceBody || flags.contains(Modifier.FINAL);
                // The only instance variables in a record tree are its header components.
                if (!isStatic && owner.getKind() == Tree.Kind.RECORD) {
                    yield null;
                }
                yield isStatic
                        ? (isFinal ? Group.STATIC_FINAL_FIELD : Group.STATIC_FIELD)
                        : (isFinal ? Group.FINAL_FIELD : Group.FIELD);
            }
            case ClassTree type ->
                interfaceBody
                                || type.getKind() != Tree.Kind.CLASS
                                || type.getModifiers().getFlags().contains(Modifier.STATIC)
                        ? Group.STATIC_TYPE
                        : Group.INNER_TYPE;
            case MethodTree method -> {
                if (method.getReturnType() == null) {
                    yield Group.CONSTRUCTOR;
                }
                yield method.getModifiers().getFlags().contains(Modifier.STATIC) ? Group.STATIC_METHOD : Group.METHOD;
            }
            default -> null;
        };
    }
}
