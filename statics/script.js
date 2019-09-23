var canvas = document.getElementById("canvas");
var defaultBoardSize = 1200;
const scale = 1000.0;
const offset = 10.0;
var _radius = 2.0;
var voterHeatmapOpacity = 0.3;
var distrctsOpacity = 0.8;
var boardsize;
var partyColor = [
  [255, 0, 0],
  [0, 0, 255],
  [0, 255, 0],
];
var voters;
var districts;
var vdList = [], dColor = [], voting = [], seats = []; // voters by districts
var loaded = false;

var drawHeatmap = false;
var drawDistricts = true;
var dp = [true, false, false];
var pcp = [0, 0, 0];
var stats = [[0,0,0,0,0],[0,0,0,0,0],[0,0,0,0,0]];

const multiply = (rgb1, rgb2) => rgb1.map((c, i) => Math.floor(c * rgb2[i] / 255))
const soften = (rgb, coeff) => rgb.map((c) => Math.floor(255 * (1 - coeff) + c * coeff));

function transform(coordinate) {
  var [x, y] = coordinate;
  return [offset + x / scale * boardsize, boardsize - y / scale * boardsize];
}

function sign(x) {
  return x > 1e-8 ? 1 : x < -1e-8 ? -1 : 0;
}

function cross(x1, y1, x2, y2) {
  return x1 * y2 - x2 * y1;
}

function lineIntersection(x1, y1, x2, y2, x3, y3, x4, y4) {
  return (sign(cross(x3 - x1, y3 - y1, x2 - x1, y2 - y1)) * sign(cross(x4 - x1, y4 - y1, x2 - x1, y2 - y1)) == -1)
      && (sign(cross(x1 - x3, y1 - y3, x4 - x3, y4 - y3)) * sign(cross(x2 - x3, y2 - y3, x4 - x3, y4 - y3)) == -1)
}

function insidePolygon(x, y, polygon) {
  var ax = x + 5000 + Math.random() * 1000;
  var ay = y + 5000 + Math.random() * 1000;
  var i, s = 0;
  for (i = 0; i < polygon.length; ++ i) {
    if (lineIntersection(x, y, ax, ay, polygon[i][0], polygon[i][1], polygon[(i+1)%polygon.length][0], polygon[(i+1)%polygon.length][1])) ++ s;
  }
  return s % 2 == 1;
}

function onFileUpload() {
  var fileToLoad = document.getElementById("mapdata").files[0];

  var fileReader = new FileReader();
  fileReader.onload = function(fileLoadedEvent){
    logStatus("Loading data");
    loadData(fileLoadedEvent.target.result);
  };

  fileReader.readAsText(fileToLoad, "UTF-8");
}

function loadData(data) {
  loaded = false;
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
    vdList.push([]);
    dColor.push("rgba(255,255,255,0)");
    var tmp = new Array(voters[0].length - 2);
    tmp.fill(0);
    voting.push(tmp);
    tmp = new Array(voters[0].length - 2);
    tmp.fill(0);
    seats.push(tmp);
  }

  for (i = 0; i < voters.length; ++ i)
    for (j = 0; j < districts.length; ++ j)
      if (insidePolygon(voters[i][0], voters[i][1], districts[j])) {
        vdList[j].push(i);
        break;
      }
  loaded = true;
  drawMap();
}

function drawPolygon(coordinates, color) {
  ctx = canvas.getContext('2d');
  ctx.globalCompositeOperation = "source-over";
  ctx.beginPath();
  ctx.fillStyle = color;
  ctx.linewidth = 0.1;
  ctx.strokeStyle="black";
  var x, y;
  [x, y] = transform(coordinates[0]);
  ctx.moveTo(x, y);
  for (var i = 1; i < coordinates.length; ++ i) {
    [x, y] = transform(coordinates[i]);
    ctx.lineTo(x, y);
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
  var i;
  // const blend = (rgb1, rgb2) => rgb1.map((c, i) => Math.floor((1 - (1-c/255) * (1-rgb2[i]/255)) * 255))
  // const blend = (rgb1, rgb2) => rgb1.map((c, i) => Math.floor(c + rgb2[i]))
  // const invert = (rgb) => rgb.map((c) => (255 - c));
  var c = [255,255,255];
  for (i = 2; i < voter.length; ++ i)
    if (dp[i - 2]) {
      var coeff = (voter[i] + pcp[i - 2]) / 2;
      // var c = soften(partyColor[i - 2], coeff);
      c = multiply(soften(partyColor[i - 2], coeff), c);
      // console.log(c);
    }
  var color = 'rgba(' + c[0] + ',' + c[1] + ',' + c[2] + ',' + voterHeatmapOpacity + ')';
  ctx.beginPath();
  ctx.fillStyle = color;
  ctx.arc(x, y, _radius, 0, Math.PI * 2);
  ctx.fill();
}

function logStatus(status) {
  document.getElementById("status").innerHTML = status;
}

function argmax(a) {
  var i, r = 0, c = 1;
  for (i = 1; i < a.length; ++ i)
    if (a[i] > a[r]) {
      r = i; c = 1;
    } else if (a[i] == a[r]) {
      ++ c;
      if (Math.random() < 1 / c)
        r = i;
    }
  return r;
}

function defaultVoting(voting) {
  var i, j, k, s = voting.reduce((a, b) => (a + b), 0);
  var t = Math.ceil(s / 4);
  var r = new Array(voting.length);
  var l = voting.slice(0);
  r.fill(0);
  for (j = 0; j < 3; ++ j) {
    k = argmax(l);
    r[k] ++;
    l[k] -= Math.min(t, l[k]);
  }
  return r;
}

function getColor(seats) {
  var t = seats.reduce((a,b)=>(a+b), 0);
  var i, c = [255,255,255];
  for (i = 0; i < seats.length; ++ i)
    if (dp[i])
        c = multiply(c, soften(partyColor[i], seats[i] / t));
  return "rgba(" + c[0] + "," + c[1] + "," + c[2] + "," + distrctsOpacity + ")";
}

function Election() {
  logStatus("Election in action!");
  stats.forEach((x) => (x.fill(0)));
  var i, j, k, p;
  for (i = 0; i < vdList.length; ++ i) {
    voting[i].fill(0);
    for (j = 0; j < vdList[i].length; ++ j) {
      k = vdList[i][j];
      p = argmax(voters[k].slice(2).map((v, i) => (v + pcp[i])));
      voting[i][p] ++;
    }
    seats[i] = defaultVoting(voting[i]);

    seats[i].forEach(function(x, i) {
      stats[i][x] ++;
      stats[i][4] += parseInt(x);
    });
    dColor[i] = getColor(seats[i]);
  }
  var t = document.getElementById("result");
  for (i = 0; i < stats.length; ++ i) {
    for (j = 0; j < 5; ++ j)
      t.rows[i + 1].cells[j + 1].innerText = stats[i][j];
  }
}

function drawPalette(ctx) {
  ctx.font = "12px Ariel";
  var i, j;
  for (i = 0; i < partyColor.length; ++ i) {
    ctx.fillStyle = "black";
    ctx.fillText("Party " + (i + 1) + ":", 30, 30 * (i + 1) + 15);
    for (j = 0; j < 4; ++ j) {
      ctx.beginPath();
      ctx.strokeStyle = "black";
      ctx.rect(80 + 40 * j, 30 * (i + 1), 25, 25);
      var c = soften(partyColor[i], j / 3);
      // console.log("rgba(" + c[0] + "," + c[1] + "," + c[2] + "," + distrctsOpacity + ")");
      ctx.fillStyle = "rgba(" + c[0] + "," + c[1] + "," + c[2] + "," + distrctsOpacity + ")";
      ctx.stroke();
      ctx.fill();
      ctx.closePath();
    }
  }
}

function drawMap() {
  ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  drawPalette(ctx);
  if (!loaded) {
    logStatus("No data is loaded");
    return;
  }
  var x = Math.max(600, parseInt(document.getElementById("boardsize").value));
  if (isNaN(x)) x = 600;
  canvas.width = x;
  canvas.height = x;
  boardsize = x - offset;
  document.getElementById("boardsize").value = x;
  _radius = 2.0 / 2400 * boardsize;

  x = Math.min(1, Math.max(0, parseFloat(document.getElementById("opacity").value)));
  if (isNaN(x)) x = 0.3;
  voterHeatmapOpacity = x;
  document.getElementById("opacity").value = x;


  console.log("Hello!");
  var board = [[0.0, 0.0], [1000.0, 0.0], [500.0, Math.sqrt(3) * 500]];
  drawPolygon(board, "rgba(255,255,255,0)");

  Election();
  if (voters && drawHeatmap) {
    logStatus("Generating heatmap");
    voters.forEach( function (voter) { drawVoter(voter); } );
  }

  if (districts && drawDistricts) {
    logStatus("Generating districts map");
    districts.forEach(
      function(district, i) { drawPolygon(district, dColor[i]); }
    );
  }
  drawPalette(ctx);
  logStatus("Done!");
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
    document.getElementById("status").innerHTML = "Cannot retrieve data, please upload your data file.";
  });
  xhttp.onerror = (function() {
    document.getElementById("status").innerHTML = "Cannot retrieve data, please upload your data file.";
  });
  xhttp.ontimeout = (function() {
      if (retries == 0) {
        console.log("No data from server");
        document.getElementById("status").innerHTML = "Cannot retrieve data, please upload your data file.";
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

function downloadImage() {
  var d = canvas.toDataURL("image/png");
  var w=window.open('about:blank','image from canvas');
  w.document.write("<img src='"+d+"' alt='from canvas'/>");
}

window.onload = function() {
  document.getElementById("boardsize").value = defaultBoardSize;
  canvas.width = defaultBoardSize;
  canvas.height = defaultBoardSize;
  boardsize = canvas.width - 10;
  ajax(10, 1000);
  drawMap();

  document.getElementById("toggleHeatmap").onchange = function() {
    drawHeatmap = document.getElementById("toggleHeatmap").checked;
  }
  document.getElementById("toggleDistricts").onchange = function() {
    drawDistricts = document.getElementById("toggleDistricts").checked;
  }


  document.getElementById("hp1").onchange = function() {
    dp[0] = document.getElementById("hp1").checked;
  }
  document.getElementById("hp2").onchange = function() {
    dp[1] = document.getElementById("hp2").checked;
  }
  document.getElementById("hp3").onchange = function() {
    dp[2] = document.getElementById("hp3").checked;
  }

  document.getElementById("p1p").oninput = function() {
    pcp[0] = document.getElementById("p1p").value / 1000.;
    document.getElementById("p1pv").innerHTML = pcp[0].toFixed(3);
  }
  document.getElementById("p2p").oninput = function() {
    pcp[1] = document.getElementById("p2p").value / 1000.;
    document.getElementById("p2pv").innerHTML = pcp[1].toFixed(3);
  }
  document.getElementById("p3p").oninput = function() {
    pcp[2] = document.getElementById("p3p").value / 1000.;
    document.getElementById("p3pv").innerHTML = pcp[2].toFixed(3);
  }
}
