package org.jboss.seven2six;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * A plugin that translates JDK 7 classes to JDK 6.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Mojo(name = "transform", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES)
public class TranslatorMojo extends AbstractMojo {

    /**
     * The output directory where resources should be processed
     */
    @Parameter(defaultValue = "${project.build.outputDirectory},${project.build.testOutputDirectory}",
            property = "seven2six.target.dirs", required = true)
    private File[] outputDirectories;

    /**
     * File patterns to include when processing
     */
    @Parameter(property = "seven2six.excludes")
    private String[] excludes;

    /**
     * File patterns to exclude when processing
     */
    @Parameter(defaultValue = "**/*.class", property = "seven2six.includes")
    private String[] includes;

    @Parameter(defaultValue = "false", property = "seven2six.transform.skip")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Log log = getLog();
        if (skip) {
            log.info("Skipping seven2six transform");
        } else {
            // Check that the output directories were defined
            if (outputDirectories != null && outputDirectories.length > 0) {
                final Translator translator = new Translator();
                final File[] files = getFiles();

                if (log.isDebugEnabled()) {
                    final String newLine = String.format("%n\t");
                    final StringBuilder sb = new StringBuilder("Transforming Files:");
                    sb.append(newLine);
                    for (File file : files) {
                        sb.append(file.getAbsolutePath()).append(newLine);
                    }
                    log.debug(sb);
                }

                translator.transformRecursive(files);
            } else {
                log.warn("No output directories were defined.");
            }
        }
    }

    private File[] getFiles() {
        final List<File> result = new ArrayList<File>();
        for (File outputDirectory : outputDirectories) {
            // If an invalid property was a defined this could result in a null value
            if (outputDirectory == null) {
                getLog().warn("A null output directory was found.");
            } else if (outputDirectory.exists()) {
                result.addAll(getFiles(outputDirectory));
            } else {
                getLog().warn(String.format("Skipping directory '%s' as it does not exist.", outputDirectory.getAbsolutePath()));
            }
        }
        return result.toArray(new File[result.size()]);
    }

    private List<File> getFiles(final File outputDirectory) {
        final List<File> result = new ArrayList<File>();
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(outputDirectory);
        scanner.setIncludes(includes);
        scanner.setExcludes(excludes);
        scanner.scan();
        for (String filename : scanner.getIncludedFiles()) {
            // Only class files
            final File targetFile = new File(outputDirectory, filename);
            if (targetFile.exists()) {
                result.add(targetFile.getAbsoluteFile());
            }
        }
        return result;
    }
}
