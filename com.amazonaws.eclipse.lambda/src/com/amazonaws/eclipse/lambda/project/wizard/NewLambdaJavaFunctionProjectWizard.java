package com.amazonaws.eclipse.lambda.project.wizard;

import static com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil.refreshProject;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.template.CodeTemplateManager;
import com.amazonaws.eclipse.lambda.project.template.data.HandlerClassTemplateData;
import com.amazonaws.eclipse.lambda.project.template.data.HandlerTestClassTemplateData;
import com.amazonaws.eclipse.lambda.project.wizard.model.NewLambdaJavaFunctionProjectWizardDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.page.NewLambdaJavaFunctionProjectWizardPageOne;
import com.amazonaws.eclipse.lambda.project.wizard.page.NewLambdaJavaFunctionProjectWizardPageTwo;
import com.amazonaws.eclipse.lambda.project.wizard.util.BrowserUtil;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;
import com.amazonaws.eclipse.lambda.project.wizard.util.JavaPackageName;

import freemarker.template.Template;

@SuppressWarnings("restriction")
public class NewLambdaJavaFunctionProjectWizard extends NewElementWizard implements INewWizard {

    private final NewLambdaJavaFunctionProjectWizardDataModel dataModel = new NewLambdaJavaFunctionProjectWizardDataModel();
    private NewLambdaJavaFunctionProjectWizardPageOne pageOne;
    private NewLambdaJavaFunctionProjectWizardPageTwo pageTwo;

    @Override
    public void addPages() {
        if (pageOne == null) {
            pageOne = new NewLambdaJavaFunctionProjectWizardPageOne(dataModel);
        }
        addPage(pageOne);

        if (pageTwo == null) {
            pageTwo = new NewLambdaJavaFunctionProjectWizardPageTwo(pageOne);
        }
        // We create pageTwo so that we can use the APIs provided by the system wizard.
        // But in the UI, we hide this page to keep the wizard simple.
    }

    @Override
    protected void finishPage(IProgressMonitor monitor)
            throws InterruptedException, CoreException {
        pageTwo.performFinish(monitor);

        monitor.setTaskName("Configuring AWS Lambda Java project");

        IJavaProject javaProject = pageTwo.getJavaProject();
        final IProject project = javaProject.getProject();

        Display.getDefault().syncExec(new Runnable() {

            public void run() {
                savePreferences(dataModel, LambdaPlugin.getDefault().getPreferenceStore());

                addSourceToProject(project, dataModel);

                File readmeFile = null;
                if (dataModel.isShowReadmeFile()) {
                    readmeFile = addReadmeFileToProject(project, dataModel.collectHandlerTestTemplateData());
                }

                refreshProject(project);

                IFile handlerClass = findHandlerClassFile(project, dataModel);
                selectAndReveal(handlerClass);

                try {
                    openHandlerClassEditor(handlerClass);
                } catch (Exception e) {
                    LambdaPlugin.getDefault().warn(
                            "Failed to open the handler class in the editor", e);
                }

                if (readmeFile != null) {
                    try {
                        BrowserUtil.openInternalBrowserAsEditor(readmeFile.toURI().toURL());
                    } catch (Exception e) {
                        LambdaPlugin.getDefault().warn(
                                "Failed to open README.html for the new Lambda project", e);
                    }
                }
            }
        });
    }

    private static IFile findHandlerClassFile(IProject project,
            NewLambdaJavaFunctionProjectWizardDataModel dataModel) {

        IPath handlerPath = new Path("src");
        JavaPackageName handlerPackage = JavaPackageName.parse(dataModel
                .getHandlerPackageName());
        for (String component : handlerPackage.getComponents()) {
            handlerPath = handlerPath.append(component);
        }
        handlerPath = handlerPath.append(dataModel.getHandlerClassName()
                + ".java");

        return project.getFile(handlerPath);
    }

    private static void openHandlerClassEditor(IFile handlerFile)
            throws PartInitException {

        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();

        IDE.openEditor(page, handlerFile, true);
    }

    @Override
    public IJavaElement getCreatedElement() {
        return pageTwo.getJavaProject();
    }

    private static void savePreferences(
            NewLambdaJavaFunctionProjectWizardDataModel dataModel,
            IPreferenceStore prefStore) {
        prefStore.setValue(
                LambdaPlugin.PREF_K_SHOW_README_AFTER_CREATE_NEW_PROJECT,
                dataModel.isShowReadmeFile());
    }

    private static void addSourceToProject(IProject project,
            NewLambdaJavaFunctionProjectWizardDataModel dataModel) {

        // Add handler class
        HandlerClassTemplateData handlerClassData = dataModel.collectHandlerTemplateData();
        addHandlerClassToProject(project, handlerClassData);

        // Add handler test class
        HandlerTestClassTemplateData handlerTestClassData = dataModel.collectHandlerTestTemplateData();
        addHandlerTestClassToProject(project, handlerTestClassData);
        addTestContextToProject(project, handlerTestClassData);

        if (dataModel.getPredefinedHandlerInputType() != null) {
            addTestUtilsToProject(project, handlerTestClassData);
        }

        // Add input json file if the user selects the predefined input type
        if (dataModel.getPredefinedHandlerInputType() != null) {
            String jsonFileName = dataModel.getPredefinedHandlerInputType()
                    .getSampleInputJsonFile();
            addSampleInputJsonFileToProject(project,
                    handlerTestClassData.getPackageName(), jsonFileName);
        }

        addTestDirectoryToClasspath(project);
    }

    private static void addHandlerClassToProject(IProject project,
            HandlerClassTemplateData templateData) {

        try {
            Template handlerTemplate = CodeTemplateManager.getInstance()
                    .getHandlerClassTemplate();
            String fileContent = CodeTemplateManager.processTemplateWithData(
                    handlerTemplate, templateData);

            FunctionProjectUtil.addSourceClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    templateData.getHandlerClassName(),
                    fileContent);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add source to the new Lambda function project",
                    e);
            throw new RuntimeException(e);
        }
    }

    private static void addHandlerTestClassToProject(IProject project,
            HandlerTestClassTemplateData templateData) {

        try {
            Template testTemplate = CodeTemplateManager.getInstance()
                    .getHandlerTestClassTemplate();

            String fileContent = CodeTemplateManager.processTemplateWithData(
                    testTemplate, templateData);

            FunctionProjectUtil.addTestClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    templateData.getHandlerTestClassName(),
                    fileContent);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add test class to the new Lambda function project",
                    e);
            throw new RuntimeException(e);
        }
    }

    private static void addTestContextToProject(
            IProject project,
            HandlerTestClassTemplateData templateData) {

        try {
            Template template = CodeTemplateManager.getInstance()
                    .getTestContextTemplate();

            String content = CodeTemplateManager.processTemplateWithData(
                    template,
                    templateData);

            FunctionProjectUtil.addTestClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    "TestContext",
                    content);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add test context to the new Lambda function project",
                    e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    private static void addTestUtilsToProject(
            IProject project,
            HandlerTestClassTemplateData templateData) {

        try {
            Template template = CodeTemplateManager.getInstance()
                    .getTestUtilsTemplate();

            String content = CodeTemplateManager.processTemplateWithData(
                    template,
                    templateData);

            FunctionProjectUtil.addTestClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    "TestUtils",
                    content);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add test utils to the new Lambda function project",
                    e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    private static void addSampleInputJsonFileToProject(IProject project,
            String testPackageName, String jsonFileName) {

        try {
            Template jsonFileTemplate = CodeTemplateManager.getInstance()
                    .getTestInputJsonFileTemplate(jsonFileName);
            String fileContent = CodeTemplateManager.processTemplateWithData(
                    jsonFileTemplate, null);

            FunctionProjectUtil.addTestResourceToProject(
                    project,
                    JavaPackageName.parse(testPackageName),
                    jsonFileName,
                    fileContent);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add test resource to the new Lambda function project",
                    e);
            throw new RuntimeException(e);
        }
    }

    private static File addReadmeFileToProject(IProject project,
            HandlerTestClassTemplateData templateData) {

        try {
            Template readmeFileTemplate = CodeTemplateManager.getInstance()
                    .getReadmeHtmlFileTemplate();
            String fileContent = CodeTemplateManager.processTemplateWithData(
                    readmeFileTemplate, templateData);

            return FunctionProjectUtil.addReadmeFileToProject(project, fileContent);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add README.html to the new Lambda function project",
                    e);
            throw new RuntimeException(e);
        }
    }

    private static void addTestDirectoryToClasspath(IProject project) {

        try {
            IJavaProject javaProj = JavaCore.create(project);
            IFolder tstFolder = project.getFolder("tst");

            IPackageFragmentRoot tstRoot = javaProj.getPackageFragmentRoot(tstFolder);

            IClasspathEntry[] originalCp = javaProj.getRawClasspath();
            IClasspathEntry[] augmentedCp = new IClasspathEntry[originalCp.length + 1];
            System.arraycopy(originalCp, 0, augmentedCp, 0, originalCp.length);

            augmentedCp[originalCp.length] = JavaCore.newSourceEntry(tstRoot.getPath());

            javaProj.setRawClasspath(augmentedCp, null);

        } catch (Exception e) {
            LambdaPlugin.getDefault().warn(
                    "Failed to add tst directory to the classpath", e);
        }
    }

}
