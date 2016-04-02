package com.github.wrdlbrnft.proguardannotations

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException

/**
 * Created by Xaver on 02/04/16.
 */
class ProguardAnnotationsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def variants = determineVariants(project)
        ensureAptPluginIsApplied(project)

        project.afterEvaluate {
            project.android[variants].all { variant ->
                configureVariant(project, variant)
            }
        }

        project.dependencies {
            compile 'com.github.wrdlbrnft:proguard-annotations-api:0.1.0.6'
            apt 'com.github.wrdlbrnft:proguard-annotations-processor:0.1.0.6'
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

    private static void ensureAptPluginIsApplied(Project project) {
        if (!project.plugins.findPlugin('com.neenbedankt.android-apt')) {
            throw new ProjectConfigurationException('Android APT Plugin is required for ProguardAnnotations to work. ' +
                    'You need to apply it before applying proguard-annotations', null)
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

    private static void configureVariant(Project project, def variant) {
        def rootOutputDir = project.file(new File(project.buildDir, "generated/source/apt"))
        def variantOutputDir = new File(rootOutputDir, variant.dirName)
        def javaCompile = variant.hasProperty('javaCompiler') ? variant.javaCompiler : variant.javaCompile
        javaCompile.doLast {
            final File generatedProguardFile = new File(project.rootDir, 'generated-proguard-rules.pro')
            generatedProguardFile.withWriter { out ->
                out.println '-keepattributes InnerClasses, Signature, Annotations'
                findProguardFiles(variantOutputDir).each {
                    out.println it.text
                }
            }
        }
    }
}
