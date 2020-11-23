AWS Toolkit for Eclipse
=======================

The **AWS Toolkit for Eclipse** is an open source plug-in for the Eclipse Java IDE that makes it easier for developers to develop, debug, and deploy Java applications using [Amazon Web Services](https://aws.amazon.com/). With the AWS Toolkit for Eclipse, you’ll be able to get started faster and be more productive when building AWS applications.

* [Homepage](https://aws.amazon.com/eclipse/)
* [User Guide](http://docs.aws.amazon.com/toolkit-for-eclipse/v1/user-guide/welcome.html) ([Source](https://github.com/awsdocs/aws-toolkit-eclipse-user-guide))
* [Getting Started](http://docs.aws.amazon.com/toolkit-for-eclipse/v1/user-guide/getting-started.html)
* [Blog Posts](https://aws.amazon.com/blogs/developer/category/java/)
* [Issues](https://github.com/aws/aws-toolkit-eclipse/issues)
* [Troubleshooting](http://docs.aws.amazon.com/toolkit-for-eclipse/v1/user-guide/trouble-shooting.html)

# Components

The Eclipse plugins included in the AWS Toolkit for Eclipse.

* [com.amazonaws.eclipse.core](https://github.com/aws/aws-toolkit-eclipse/tree/master/bundles/com.amazonaws.eclipse.core) (AWS Toolkit for Eclipse Core)
* [com.amazonaws.eclipse.sdk.ui](https://github.com/aws/aws-toolkit-eclipse/tree/master/bundles/com.amazonaws.eclipse.sdk.ui) (AWS Toolkit for Eclipse UI)
* [com.amazonaws.eclipse.ec2](https://github.com/aws/aws-toolkit-eclipse/tree/master/bundles/com.amazonaws.eclipse.ec2) (AWS Toolkit for Eclipse EC2)
* [com.amazonaws.eclipse.dynamodb](https://github.com/aws/aws-toolkit-eclipse/tree/master/bundles/com.amazonaws.eclipse.dynamodb) (AWS Toolkit for Eclipse DynamoDB)
* [com.amazonaws.eclipse.identitymanagement](https://github.com/aws/aws-toolkit-eclipse/tree/master/bundles/com.amazonaws.eclipse.identitymanagement) (AWS Toolkit for Eclipse IAM)
* [com.amazonaws.eclipse.cloudformation](https://github.com/aws/aws-toolkit-eclipse/tree/master/bundles/com.amazonaws.eclipse.cloudformation) (AWS Toolkit for Eclipse CloudFormation)
* [com.amazonaws.eclipse.codecommit](https://github.com/aws/aws-toolkit-eclipse/tree/master/bundles/com.amazonaws.eclipse.codecommit) (AWS Toolkit for Eclipse CodeCommit)
* [com.amazonaws.eclipse.codedeploy](https://github.com/aws/aws-toolkit-eclipse/tree/master/bundles/com.amazonaws.eclipse.codedeploy) (AWS Toolkit for Eclipse CodeDeploy)
* [com.amazonaws.eclipse.codestar](https://github.com/aws/aws-toolkit-eclipse/tree/master/bundles/com.amazonaws.eclipse.codestar) (AWS Toolkit for Eclipse CodeStar)
* [com.amazonaws.eclipse.elasticbeanstalk](https://github.com/aws/aws-toolkit-eclipse/tree/master/bundles/com.amazonaws.eclipse.elasticbeanstalk) (AWS Toolkit for Eclipse Elastic Beanstalk)
* [com.amazonaws.eclipse.lambda](https://github.com/aws/aws-toolkit-eclipse/tree/master/bundles/com.amazonaws.eclipse.lambda) (AWS Toolkit for Eclipse Lambda)
* [com.amazonaws.eclipse.opsworks](https://github.com/aws/aws-toolkit-eclipse/tree/master/bundles/com.amazonaws.eclipse.opsworks) (AWS Toolkit for Eclipse OpsWorks)
* [com.amazonaws.eclipse.rds](https://github.com/aws/aws-toolkit-eclipse/tree/master/bundles/com.amazonaws.eclipse.rds) (AWS Toolkit for Eclipse RDS)
* [com.amazonaws.eclipse.simpledb](https://github.com/aws/aws-toolkit-eclipse/tree/master/bundles/com.amazonaws.eclipse.simpledb) (AWS Toolkit for Eclipse SimpleDB)

# Contributing

## Requirements

To contribute to the AWS Toolkit for Eclipse, you need to have at least these requirements met.

1. Java 8
2. [git](https://git-scm.com/), [maven 3.3+](https://maven.apache.org/)
3. Eclipse IDE 4.8 (Photon) or newer
4. [EGit](https://www.eclipse.org/egit/) 3.4.2+, [M2Eclipse](https://www.eclipse.org/m2e/) 1.5.1+ (Note: these two plugins are part of the standard Eclipse)
5. [WebTools](https://www.eclipse.org/webtools/)

## Build and Test with Maven

1. Checkout the source code: ```git clone https://github.com/aws/aws-toolkit-eclipse```
2. Step into the target folder: ```cd aws-toolkit-eclipse```
3. Install the third-party dependencies and build the target platform: ```sh setup.sh```
4. Build and test with Maven using Photon platform: ```mvn -Pbuild-eclipse clean verify```
5. Build and test with Maven using 2018.9 platform: ```mvn -Pbuild-eclipse -Declipse.target=20189 clean verify```
5. Build and test with Maven using 2019.6 platform: ```mvn -Pbuild-eclipse -Declipse.target=20196 clean verify```
5. Build and test with Maven using 2019.9 platform: ```mvn -Pbuild-eclipse -Declipse.target=20199 clean verify```
6. Build and test with Maven using 2020.3 platform: ```mvn -Pbuild-eclipse -Declipse.target=20203 clean verify```
6. Build and test with Maven using 2020.9 platform: ```mvn -Pbuild-eclipse -Declipse.target=20209 clean verify```

**note, 2019-3 and 2019-12 do not build because we reach into Eclipse internals, but it will work on those versions**

## Import into Eclipse and Set up Development Environment

You must have [EGit](https://www.eclipse.org/egit/) installed to your Eclipse IDE.

1. Open Eclipse, and choose *File, Import, Git, Projects from Git*, then choose *Next*.
2. Open EGit Import Wizard, then choose *Clone URI, Next*.
3. Type [**https://github.com/aws/aws-toolkit-eclipse**](https://github.com/aws/aws-toolkit-eclipse) in the *URI* field, and enter your Github user name and password. Then, choose *Next*.
4. Select *master* branch, and specify the destination directory in the next two pages, and choose *Next*.
5. Select *Import existing Eclipse projects*, and then choose *Next*.
6. Select *Search for nested projects*, you would see all the plugin projects and feature projects etc. Select the projects you are interested in or all the projects, and then choose *Finish*.

Now you have imported all the plugin projects into Eclipse, but you need to set up the target platform since the plugins have dependencies which are not the standard OSGi bundles.

1. Open the terminal and step into the root folder of the repository.
2. Run the *setup.sh* script if you haven't created the target platform: ```sh setup.sh```.
3. Back to Eclipse, open *Preferences* page, choose ```Plug-in Development -> Target Platform```
4. In the *Target Platform* page, choose ```Add -> Next -> Add -> Software Site -> Add -> Local``` to select the target repository you just built. This repository is located at ```aws-toolkit-eclipse/releng/com.amazonaws.eclipse.devide/target/repository```.
5. Type a name for this repository, say *aws devide*, and choose ```Finish```. Select ```Uncategorized``` and choose ```Finish```
6. Type a name for this target platform, say *AWS Target Platform*, and choose ```OK```. Use this new target platform, and you would be able to run the AWS plugins under this target platform.
7. It will rebuild the workspace with the target platform. If you see `plugin execution not covered by lifecycle configuration`, right click on the problem and select `Quick Fix -> Discover new m2e connectors`

**Notice: If you imported the *com.amazonaws.eclipse.javasdk* project into your Eclipse workspace when you are checking in the repo, you may need to close that project as it  overrides the *Java SDK* bundle in the target platform. If the platform is set up correctly, you will see no errors in `Markers`**

## Build and test the IDE

1. Right click on the aws-toolkit-for-eclipse node `run as -> run configurations`
2. Add an `eclipse application` configuration
3. In `Run a product` select `org.eclipse.platform.ide`
4. In the plug ins tab make sure to select `All workspace enabled plugins and features`
5. Press run, it should build and run

# Getting Help

* You can submit issues for bug reports and feature requests by using Github [issues](https://github.com/aws/aws-toolkit-eclipse/issues).
* You can also send us email at <aws-eclipse-feedback@amazon.com> to send feedback or report issues.
