{
  "Description": "Image tagger",
  "Parameters" : {
    "ImageBucketExists" : {
      "Type" : "String",
      "Description" : "Whether the specified ImageBucket exist or not",
      "Default" : "false",
      "AllowedValues": ["true", "false"]
    },
    "ImageBucketName" : {
        "Type" : "String",
        "Description" : "Name of S3 bucket used to hold images for tagging.",
        "Default" : "image-bucket",
        "MinLength" : "1"
    }
  },
  "Conditions" : {
    "CreateImageBucket" : {"Fn::Equals" : [{"Ref" : "ImageBucketExists"}, "false"]}
  },
  "Resources": {
    "ImageBucket": {
      "Type": "AWS::S3::Bucket",
      "Condition" : "CreateImageBucket",
      "Properties": {
        "BucketName" : {"Ref" : "ImageBucketName"}
      }
    },
    "TagImage": {
      "Type": "AWS::Serverless::Function",
      "Properties": {
        "Handler": "${packageName}.TagImage",
        "Runtime" : "java8",
        "CodeUri" : "./target/${artifactId}-${version}.jar",
        "Policies": [
          "AmazonS3FullAccess",
          "AmazonRekognitionFullAccess"
        ],
        "Events": {
          "ProcessNewImage": {
            "Type": "S3",
            "Properties": {
              "Bucket": {"Ref" : "ImageBucket"},
              "Events": "s3:ObjectCreated:*",
              "Filter": {
                "S3Key": {
                  "Rules": [{"Name": "suffix", "Value": ".jpg"}]
                }
              }
            }
          }
        }
      }
    }
  },
  "Outputs" : {
    "ImageBucket" : {
      "Value" : { "Ref" : "ImageBucketName" }
    }
  }
}
