var canvas = document.getElementById("canvas");
const scale = 1000.0;
const offset = 10.0;
const _radius = 3.0;
var boardsize;
var prefColor = [
    [255, 0, 0],
    [0, 255, 0],
    [0, 0, 255],
];
var voters;
var districts;

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

function loadData(data) {
    voters = [];
    districts = [];
    var lines = data.split("\n");
    var n, p, i, j, it = 0, m;
    [n, p] = lines[it].split(" ").map(x => parseInt(x));
    for (it = 1; it <= n; ++ it) {
        voters.push(lines[it].split(" ").map(x => parseFloat(x)));
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
    drawMap();
    // TODO
}

function drawPolygon(coordinates, color) {
    ctx = canvas.getContext('2d');
    ctx.beginPath();
    ctx.fillStyle = color;
    ctx.linewidth = 0.1;
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

function drawVoter(voter) {
    ctx = canvas.getContext('2d');
    ctx.globalCompositeOperation = "multiply";
    var i, x, y;
    [x,y] = transform([voter[0], voter[1]]);

    ctx.beginPath();
    ctx.fillStyle = "red";
    ctx.arc(x, y, _radius, 0, Math.PI * 2);
    ctx.fill();
    // console.log(voter);
    // for (i = 2; i < voter.length; ++ i) {
    //     var radgrad = ctx.createRadialGradient(x,y,0,x,y,_radius);
    //     var strength = voter[i - 2];
    //     var r = prefColor[i - 2][0], g = prefColor[i - 2][1], b = prefColor[i - 2][2];
    //     radgrad.addColorStop(0, 'rgba(' + r + ',' + g + ',' + b + ',' + strength + ')');
    //     radgrad.addColorStop(0, 'rgba(' + r + ',' + g + ',' + b + ',' + strength * 0.5 + ')');
    //     radgrad.addColorStop(0, 'rgba(' + r + ',' + g + ',' + b + ',' + 0 + ')');
    //     // draw shape
    //     ctx.fillStyle = radgrad;
    //     ctx.fillRect(0,0,canvas.width,canvas.height);
    // }
}

function drawMap() {
    ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    // ctx.globalAlpha = 0.2;

    console.log("Hello!");
    var board = [[0.0, 0.0], [1000.0, 0.0], [500.0, Math.sqrt(3) * 500]];
    drawPolygon(board, "rgba(255,255,255,0)");

    if (voters) {
        voters.forEach( function (voter) { drawVoter(voter); } );
    }

    if (districts) {
        districts.forEach(
            function(district) { drawPolygon(district, "rgba(255,255,255,0)"); }
        );
    }
    console.log("Finished!");
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

function resizeCanvas() {
    var x = Math.max(300, parseInt(document.getElementById("boardsize").value));
    canvas.width = x;
    canvas.height = x;
    boardsize = x - offset;
    document.getElementById("boardsize").value = x;
    drawMap();
}

window.onload = function() {
    var defaultBoardSize = 1800;
    document.getElementById("boardsize").value = defaultBoardSize;
    canvas.width = defaultBoardSize;
    canvas.height = defaultBoardSize;
    boardsize = canvas.width - 10;
    ajax(10, 1000);
    drawMap();
}
