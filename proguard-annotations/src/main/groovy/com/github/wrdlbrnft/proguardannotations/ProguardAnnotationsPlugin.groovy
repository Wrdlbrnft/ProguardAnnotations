package com.github.wrdlbrnft.proguardannotations

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException

/**
 * Created by Xaver on 02/04/16.
 */
class ProguardAnnotationsPlugin implements Plugin<Project> {

    private static final String GENERATED_RULE_FILE_NAME = 'generated-proguard-rules.pro'

    @Override
    void apply(Project project) {
        project.afterEvaluate {
            def variants = determineVariants(project)

            project.android[variants].all { variant ->
                configureVariant(project, variant)
            }

            project.android.buildTypes.each { type ->
                type.proguardFiles new File(project.buildDir, GENERATED_RULE_FILE_NAME)
            }

            project.dependencies {
                provided 'com.github.wrdlbrnft:proguard-annotations-api:0.2.0.45'
                provided 'com.github.wrdlbrnft:proguard-annotations-processor:0.2.0.45'
            }
        }
    }

    private static String determineVariants(Project project) {
        if (project.plugins.findPlugin("com.android.application")) {
            return "applicationVariants";
        } else if (project.plugins.findPlugin("com.android.library")) {
            return "libraryVariants";
        } else {
            throw new ProjectConfigurationException("The com.android.application or com.android.library plugin must be applied to the project", null)
        }
    }

    private static void configureVariant(Project project, def variant) {
        def javaCompile = variant.hasProperty('javaCompiler') ? variant.javaCompiler : variant.javaCompile
        javaCompile.doLast {
            final generatedProguardFile = new File(project.buildDir, GENERATED_RULE_FILE_NAME);
            generatedProguardFile.withWriter { out ->
                out.println '-keepattributes InnerClasses, Signature, Annotations'
                findProguardFiles(project.buildDir).each { file ->
                    def trimmedContent = file.text.trim()
                    if (!trimmedContent.isEmpty()) {
                        out.println trimmedContent
                    }
                    file.delete();
                }
            }
        }
    }

    private static Set<File> findProguardFiles(File folder) {
        def proguardFiles = new HashSet();
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                proguardFiles.addAll(findProguardFiles(file))
                continue
            }

            def fileName = file.name
            if (fileName.startsWith('generated_proguard_part_') && fileName.endsWith('.pro')) {
                proguardFiles.add(file)
            }
        }
        return proguardFiles;
    }
}
