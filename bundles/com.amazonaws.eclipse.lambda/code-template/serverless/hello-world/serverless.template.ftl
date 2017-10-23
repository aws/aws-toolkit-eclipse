{
  "Description": "A simple Lambda function saying \"Hello World\".",
  "Resources": {
    "HelloWorld": {
      "Type": "AWS::Serverless::Function",
      "Properties": {
        "Handler": "${packageName}.HelloWorld",
        "Runtime" : "java8",
        "CodeUri" : "./target/${artifactId}-${version}.jar"
      }
    }
  }
}
