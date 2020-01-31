'use strict';


// create the editor
let form = document.querySelector('#formId');
let result = document.querySelector('#resultId');
const container = document.getElementById("jsoneditor");
const options = {mode: "code"};
const editor = new JSONEditor(container, options);

// set json
const initialJson = {
    "pojo": {
        "className": "Greeting",
        "tableName": "k_greeting",
        "fields": [
            {
                "fieldName": "test",
                "fieldType": "STRING",
                "columnName": "test_column"
            },
            {
                "fieldName": "salam",
                "fieldType": "INT",
                "columnName": "k_salam"
            }
        ]
    },
    "mapperName": "GreetingMapper",
    "serviceName": "GreetingService",
    "serviceImplName": "GreetingServiceImpl",
    "controller": {
        "className": "GreetingController",
        "mapping": "greeting"
    }
};
editor.set(initialJson)

function uploadSingleFile() {
    let xhr = new XMLHttpRequest();
    xhr.open("POST", "/spring-crud/generate");
    xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    xhr.onload = function () {
        console.log(xhr.responseText);
        if (xhr.status === 200) {
            let response = JSON.parse(xhr.responseText);
            let {pojo, mapper, controller, service, serviceImpl} = response;

            let resultText = '';
            if (pojo) resultText += `<pre><code>${pojo}</code></pre>`;
            if (mapper) resultText += `<pre><code>${mapper}</code></pre>`;
            if (controller) resultText += `<pre><code>${controller}</code></pre>`;
            if (service) resultText += `<pre><code>${service}</code></pre>`;
            if (serviceImpl) resultText += `<pre><code>${serviceImpl}</code></pre>`;
            result.innerHTML = resultText;
        } else {
            console.error(xhr)
        }

    };
    xhr.send(JSON.stringify(editor.get()));
}

form.addEventListener('submit', function (event) {
    uploadSingleFile();
    event.preventDefault();
}, true);
