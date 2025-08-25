# Harness FME Java SDK update on change strategy

Set an envionment variable with your API key

export HARNESS_SERVER_SIDE_API_KEY="<your server side api key>"

You can find the key in the Admin Settings user interface.

Create a flag called "api" or change the two places it is referenced in the ApiDemo.java

``` 
mvn clean install
```

The example runs...

run.zsh

``` bash
#!/bin/zsh
source env
set -x
mvn spring-boot:run
```

Use a browser to call the APIs in your ApiDemo.java

``` html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Image Loader</title>
  <style>
    body {
      font-family: sans-serif;
      text-align: center;
      padding: 2em;
    }
    img {
      max-width: 100%;
      height: auto;
      border: 1px solid #ccc;
      border-radius: 8px;
    }
  </style>
</head>
<body>
  <h1>Loaded Image</h1>
  <p id="loading">Loading image...</p>
  <img id="loadedImage" style="display:none;" />

  <script>
    async function loadImage() {
      try {
        const response = await fetch('http://localhost:8080/api/demo/image/dmartin');
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

        const data = await response.json();
        const imageUrl = data.url;

        const img = document.getElementById('loadedImage');
        img.src = imageUrl;
        img.style.display = 'block';

        document.getElementById('loading').style.display = 'none';
      } catch (err) {
        document.getElementById('loading').textContent = 'Failed to load image.';
        console.error(err);
      }
    }

    // Trigger on load
    window.addEventListener('DOMContentLoaded', loadImage);
  </script>
</body>
</html>
```
