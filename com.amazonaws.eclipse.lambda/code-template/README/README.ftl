<html>
<head>
<style>
/*************************************
GENERAL
*************************************/
body {
    margin: 15;
    padding: 0;
    font: 12px/1.4em "Lucida Grande", Verdana, sans-serif;
    color: #333;
    overflow-y: scroll;
    text-rendering: optimizeLegibility;
    background-color: #F2F2F2;
}

h2 {
    font-size: 1.3em;
    line-height: 1.5em;
    font-weight: bold;
    margin: 20px 0 0 0;
    padding: 0;
    border-bottom: 3px solid #eee;

    /* icon setup */
    padding: 0.2em 1em 0.2em 30px;
    background-position: 0 50%;
    background-repeat: no-repeat;
}


/*************************************
SECTIONS
*************************************/
div#content {
    margin: 30px auto;
    padding: 0 30px 15px 30px;
    background-color: #fff;
    width: 940px;

    /* box-shadow */
    -moz-box-shadow: 0 5px 10px #aaa;
    -webkit-box-shadow: 0 5px 10px #aaa;
    box-shadow: 0 5px 10px #aaa;

    /* bottom corners */
    -webkit-border-bottom-right-radius: 7px;
    -webkit-border-bottom-left-radius: 7px;
    -moz-border-radius-bottomright: 7px;
    -moz-border-radius-bottomleft: 7px;
    border-bottom-right-radius: 7px;
    border-bottom-left-radius: 7px;
}

/*div#content div.section {}*/

div#content div.section ul {
    margin: 0;
    padding: 1em 0 0 2em;
    overflow: hidden;
}

div#content div.section ul li {
    list-style-type: square;
    white-space: nowrap;
    line-height: 1.5em;
}

/* Section titles */
div#content div.section.s3 h2 {
    background-image: url(../images/drive.png);
}

div#content div.section.ec2 h2 {
    background-image: url(../images/server.png);
}

div#content div.section.sdb h2 {
    background-image: url(../images/database.png);
}


/*************************************
CONTAINERS
*************************************/
.container {
    zoom: 1;
}

.container:after {
    content: ".";
    display: block;
    height: 0;
    clear: both;
    visibility: hidden;
}


/*************************************
GRIDS
*************************************/
.grid { float: left; margin-right: 20px; }
.gridlast { margin-right: 0; }
.grid5 { width: 300px; }
.grid15 { width: 940px; }

</style>
</head>
<div style="left: 32px; width: 975px; height: 753px;">
<div style="padding-left: 37.5px; padding-right: 37.5px;">

<div>

<h1>Welcome to AWS Lambda Eclipse Plugin</h1>

<p><strong>Congratulations</strong>! You have created your first AWS Lambda project. So what's next?</p>

<hr>

</div>

<div>

<h3>Step 1: Implement your function</h3>

<p>Open up <strong>${handlerClassName}.java</strong> and implement the <strong>handleRequest</strong> method. This method is the entry point for your Lambda function, and it will be invoked by Lambda in response to input from the event sources of your function.</p>

<blockquote>
  <p><strong>Note:</strong> You can add new classes and additional external dependencies in your project if needed.</p>
</blockquote>

<hr>

</div>

<div>

<h3>Step 2: Test your function</h3>

<p>Open up <strong>${handlerTestClassName}.java</strong>. Fill in your test logic to validate the input and output of your function handler, and then run it locally as a normal JUnit test.</p>

<blockquote>
  <p><strong>Note:</strong> The unit test provides a sample JSON input file if you have chosen a predefined event type as your function input. You can modify the JSON file, or create new ones based on it.</p>
</blockquote>

<hr>

</div>

<div>

<h3>Step 3: Upload your function</h3>

<p>
Under Project or Package Explorer View, right-click on your project and select <strong>Amazon Web Services -&gt; Upload Function to AWS Lambda</strong>.
Then follow the steps to create a new Lambda function or upload your code to an existing function.
</p>
</ul>

<hr>

</div>

<div>

<h3>Step 4: Invoke your function</h3>

<p>Now we are ready to run the function in the cloud. Right-click on your project again and select <strong>Amazon Web Services -&gt; Run on AWS Lambda</strong>. <br>
In the input dialog, enter the JSON input for your function, or select one of the JSON files in your project.</p>

<blockquote>
  <p><strong>Tip:</strong> You can add new JSON input files in your project, and they will show up in this dialog as long as the file name ends with ".json".</p>
</blockquote>

<p>Click <strong>Invoke</strong> and check the output of your function in the Eclipse Console View.</p>

<hr>

</div>

<div>

<h3>What's next?</h3>

<p>If you want to know more about AWS Lambda, check out the following links:</p>

<ul>
<li><a href="http://aws.amazon.com/lambda/details/">AWS Lambda Product Details</a></li>
<li><a href="http://docs.aws.amazon.com/lambda/latest/dg/welcome.html">AWS Lambda Developer Guide</a></li>
</ul>

<p><a href="mailto:aws-eclipse-feedback@amazon.com">Contact us</a> to send bug reports and feedbacks.</p>

<p><strong><em>AWS SDK for Java team</em></strong></p>

</div>

</div>

</div>
</html>