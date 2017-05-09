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

1. Java 7+
2. [git](https://git-scm.com/), [maven](https://maven.apache.org/)
3. Eclipse IDE 4.4.2 (Luna) or newer
4. [EGit](https://www.eclipse.org/egit/) 3.4.2+, [M2Eclipse](https://www.eclipse.org/m2e/) 1.5.1+ (Note: these two plugins are part of the standard Eclipse distribution since Luna)

## Build and Test with Maven

1. Checkout the source code: ```git clone https://github.com/aws/aws-toolkit-eclipse```
2. Step into the target folder: ```cd aws-toolkit-eclipse```
3. Build and test with Maven: ```mvn clean verify```

## Import into Eclipse

You must have [EGit](https://www.eclipse.org/egit/) installed to your Eclipse IDE.

1. Open Eclipse, and choose *File, Import, Git, Projects from Git*, then choose *Next*.
2. Open EGit Import Wizard, then choose *Clone URI, Next*.
3. Type [**https://github.com/aws/aws-toolkit-eclipse**](https://github.com/aws/aws-toolkit-eclipse) in the *URI* field, and enter your Github user name and password. Then, choose *Next*.
4. Select *master* branch, and specify the destination directory in the next two pages, and choose *Next*.
5. Select *Import existing Eclipse projects*, and then choose *Next*.
6. Select *Search for nested projects*, you would see all the plugin projects and feature projects etc. Select the projects you are interested in or all the projects, and then choose *Finish*.

# Getting Help

* You can submit issues for bug reports and feature requests by using Github [issues](https://github.com/aws/aws-toolkit-eclipse/issues).
* You can also send us email at <aws-eclipse-feedback@amazon.com> to send feedback or report issues.
