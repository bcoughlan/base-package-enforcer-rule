/**
 * Using this enforcer rule allows you to make assumptions about Maven module inter-dependencies based
 * on package names.
 *
 * This is an example ArchUnit test to verify that classes under an "internal" package in a Maven
 * module are not depended on by any other module.
 */
class ArchUnitExampleTest {

    private static final String PACKAGE_BASE = "com.your.root.package";
    private static final int PACKAGE_BASE_LENGTH = PACKAGE_BASE.split("\\.").length;
  
    @Test
    void testArchitecture() {
        var rule = classes().that()
                .resideInAnyPackage(PACKAGE_BASE + "..internal..")
                .should(onlyBeAccessedWithinSameMavenModule);

        rule.check(new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages(PACKAGE_BASE));
    }

    ArchCondition<JavaClass> onlyBeAccessedWithinSameMavenModule = new ArchCondition<>(
            "only be accessed within the same Maven module") {
        @Override
        public void check(JavaClass clazz, ConditionEvents events) {
            var dependents = dependentClasses(clazz)
                    .filter(dependent -> !isSameMavenModule(clazz, dependent))
                    .toList();

            if (!dependents.isEmpty()) {
                var dependentNames = dependents.stream().map(JavaClass::getFullName).toList();
                var message = "Internal class " + clazz.getFullName() + " has dependencies from other module(s): "
                        + dependentNames;
                events.add(SimpleConditionEvent.violated(clazz.getFullName(), message));
            }
        }
    };

    static Stream<JavaClass> dependentClasses(JavaClass clazz) {
        return Stream.concat(
                clazz.getDirectDependenciesToSelf().stream()
                        .map(Dependency::getOriginClass),
                clazz.getAccessesToSelf().stream()
                        .map(JavaAccess::getOriginOwner))
                .filter(c -> c != clazz)
                .distinct();
    }

    static boolean isSameMavenModule(JavaClass class1, JavaClass class2) {
        var idx1 = StringUtils.ordinalIndexOf(class1.getPackageName(), ".", PACKAGE_BASE_LENGTH);
        if (idx1 == -1)
            idx1 = class1.getPackageName().length();

        var idx2 = StringUtils.ordinalIndexOf(class2.getPackageName(), ".", PACKAGE_BASE_LENGTH);
        if (idx2 == -1)
            idx2 = class2.getPackageName().length();

        return class1.getPackageName().substring(0, idx1).equals(class2.getPackageName().substring(0, idx2));
    }

}
