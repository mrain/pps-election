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
var districts;
var hDatas;

function loadData(data) {
    voters = [];
    districts = [];
    hDatas = [];
    var lines = data.split("\n");
    var n, p, i, j, it = 0, m;
    [n, p] = lines[it].split(" ").map(x => parseInt(x));
    for (j = 0; j < p; ++ j) {
        hDatas.push([]);
    }
    for (it = 1; it <= n; ++ it) {
        var line = lines[it].split(" ").map(x => parseFloat(x));
        voters.push({
            "x" : line[0],
            "y" : line[1],
            "pref" : line.slice(2),
        });
        for (j = 0; j < p; ++ j) {
            coords = transform([line[0], line[1]]);
            hDatas[j].push([coords[0], coords[1], line[2 + j] / 2.]);
        }
    }
    m = parseInt(lines[it ++]);
    while (m --) {
        var line = lines[it ++].split(" ").map(x => parseFloat(x));
        var l = line[0];
        var district = [];
        for (i = 0; i < l; ++ i)
          district.push([line[2 * i + 1], line[2 * i + 2]]);
        districts.push(district);
    }
    // console.log(voters);
    // console.log(hDatas);
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
    // console.log(x, y);
    ctx.moveTo(x, y);
    for (var i = 1; i < coordinates.length; ++ i) {
        [x, y] = transform(coordinates[i]);
        ctx.lineTo(x, y);
        // console.log(x, y);
    }
    ctx.closePath();
    ctx.stroke();
    ctx.fill();
}

function drawMap() {
    canvas = document.getElementById('canvas');
    ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.globalAlpha = 0.2;

    if (hDatas) {
        simpleheat('canvas').data(hDatas[0]).gradient({1 : "rgba(255, 0, 0, 50)"}).draw();
        simpleheat('canvas').data(hDatas[1]).gradient({1 : "rgba(0, 255, 0, 50)"}).draw();
    }

    var board = [[0.0, 0.0], [1000.0, 0.0], [500.0, Math.sqrt(3) * 500]];
    drawPolygon(ctx, board, "rgba(255,255,255,0)");

    districts.forEach(
        function(district) {
          console.log(district);
          drawPolygon(ctx, district, "rgba(255,255,255,0)");
        }
    );

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
