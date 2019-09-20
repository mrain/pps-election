const scale = 1000.0;
const boardsize = 550.0;
const offset = 100.0;

function transform(coordinate) {
    var [x, y] = coordinate;
    return [offset + x / scale * boardsize, boardsize - y / scale * boardsize];
}

function onFileUpload() {
    var fileToLoad = document.getElementById("mapdata").files[0];

    var fileReader = new FileReader();
        fileReader.onload = function(fileLoadedEvent){
        loadData(fileLoadedEvent.target.result);
    };

    fileReader.readAsText(fileToLoad, "UTF-8");
}

var voters;
var parties;
var hDatas;

function loadData(data) {
    voters = [];
    parties = [];
    hDatas = [];
    var lines = data.split("\n");
    var n, p, i, j;
    [n, p] = lines[0].split(" ").map(x => parseInt(x));
    for (j = 0; j < p; ++ j) {
        hDatas.push([]);
    }
    for (i = 1; i <= n; ++ i) {
        var line = lines[i].split(" ").map(x => parseFloat(x));
        voters.push({
            "x" : line[0],
            "y" : line[1],
            "pref" : line.slice(2),
        });
        for (j = 0; j < p; ++ j) {
            hDatas[j].push([line[0], line[1], line[2 + j]]);
        }
    }
    console.log(voters);
    console.log(hDatas);
    drawMap();
    // TODO
}

function drawPolygon(ctx, coordinates, color) {
    ctx.beginPath();
    ctx.fillStyle = color;
    ctx.linewidth = 1;
    ctx.strokeStyle="black";
    var x, y;
    [x, y] = transform(coordinates[0]);
    console.log(x, y);
    ctx.moveTo(x, y);
    for (var i = 1; i < coordinates.length; ++ i) {
        [x, y] = transform(coordinates[i]);
        ctx.lineTo(x, y);
        console.log(x, y);
    }
    ctx.closePath();
    ctx.stroke();
    ctx.fill();
}

function drawMap() {
    canvas = document.getElementById('canvas');
    ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    if (hDatas) {
        simpleheat('canvas').data(hDatas[0]).gradient({1 : "red"}).draw();
        simpleheat('canvas').data(hDatas[1]).gradient({1 : "blue"}).draw();
    }

    var board = [[0.0, 0.0], [1000.0, 0.0], [500.0, Math.sqrt(3) * 500]];
    drawPolygon(ctx, board, "rgba(255,255,255,0)");

}

function ajax(retries, timeout) {
    console.log("Retrieving data")
    var xhttp = new XMLHttpRequest();
    xhttp.onload = (function() {
            var refresh = -1;
            try {
                if (xhttp.readyState != 4)
                    throw "Incomplete HTTP request: " + xhttp.readyState;
                if (xhttp.status != 200)
                    throw "Invalid HTTP status: " + xhttp.status;
                refresh = loadData(xhttp.responseText);
                if (latest_version < version)
                latest_version = version;
                else refresh = -1;
            } catch (message) {
                alert(message);
            }

            console.log(refresh);
        });
    xhttp.onabort = (function() {
        document.getElementById("load-status").innerHTML = "Cannot retrieve data, please upload your data file.";
    });
    xhttp.onerror = (function() {
        document.getElementById("load-status").innerHTML = "Cannot retrieve data, please upload your data file.";
    });
    xhttp.ontimeout = (function() {
            if (retries == 0) {
                console.log("No data from server");
                document.getElementById("load-status").innerHTML = "Cannot retrieve data, please upload your data file.";
            } else {
                console.log("AJAX timeout (retries: " + retries + ")");
                ajax(retries - 1, timeout * 2);
            }
        });
    xhttp.open("GET", "data.txt", true);
    xhttp.responseType = "text";
    xhttp.timeout = timeout;
    xhttp.send();
}

ajax(10, 1000);
// drawMap();
