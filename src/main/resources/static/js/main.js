'use strict';


// create the editor
let form = document.querySelector('#formId');
let result = document.querySelector('#resultId');
const container = document.getElementById("jsoneditor");
const options = {mode: "code"};
const editor = new JSONEditor(container, options);
const globalMap = {};

function htmlSpecialChars(str) {
    let map = {
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "\"": "&quot;",
        "'": "&#39;" // ' -> &apos; for XML only
    };
    return str.replace(/[&<>"']/g, function (m) {
        return map[m];
    });
}

function htmlSpecialCharsDecode(str) {
    let map = {
        "&amp;": "&",
        "&lt;": "<",
        "&gt;": ">",
        "&quot;": "\"",
        "&#39;": "'"
    };
    return str.replace(/(&amp;|&lt;|&gt;|&quot;|&#39;)/g, function (m) {
        return map[m];
    });
}

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
editor.set(initialJson);

function copyCode(id) {
    /* let copyText = document.getElementById(id);
     copyText.select();
     document.execCommand("copy");
     alert("Copied the text: " + copyText.value);*/

    const el = document.createElement('textarea');
    el.value = globalMap[id];
    el.setAttribute('readonly', '');
    el.style.position = 'absolute';
    el.style.left = '-9999px';
    document.body.appendChild(el);
    el.select();
    document.execCommand('copy');
    document.body.removeChild(el);
    alert("Copied the text: " + el.value);
}

function generateCodeBlock(id, text) {
    globalMap[id] = text;
    return `<pre><button onclick="copyCode('${id}')">copy</button><code>${htmlSpecialChars(text)}</code></pre>`;
}

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
            if (pojo) resultText += generateCodeBlock('pojoCode', pojo);
            if (mapper) resultText += generateCodeBlock('mapperCode', mapper);
            if (controller) resultText += generateCodeBlock('controllerCode', controller);
            if (service) resultText += generateCodeBlock('serviceCode', service);
            if (serviceImpl) resultText += generateCodeBlock('serviceImplCode', serviceImpl);
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
