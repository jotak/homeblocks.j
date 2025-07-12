"use strict";
angular.module('homeblocks', [
    'ngRoute',
    'ngSanitize',
    'homeblocks.loginview',
    'homeblocks.mainview',
    'homeblocks.editview'
])
    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.otherwise({ redirectTo: '/u' });
    }]);
function computePositions(blocks) {
    var map = {};
    var block, i;
    for (i in blocks) {
        block = blocks[i];
        map[block.posx + "," + block.posy] = block;
    }
    for (i in blocks) {
        block = blocks[i];
        block.N = map[block.posx + "," + (block.posy - 1)] !== undefined;
        block.S = map[block.posx + "," + (block.posy + 1)] !== undefined;
        block.E = map[(block.posx + 1) + "," + block.posy] !== undefined;
        block.W = map[(block.posx - 1) + "," + block.posy] !== undefined;
    }
}
function findBlockByPosition(blocks, x, y) {
    for (var i in blocks) {
        var block = blocks[i];
        if (block.posx == x && block.posy == y) {
            return block;
        }
    }
    return null;
}
function saveProfile($http, scope) {
    var deferred = Q.defer();
    $http.post('/api/user/' + scope.refUser + '/profile/' + scope.profile, scope.page).success(function() {
        deferred.resolve(true);
    }).error(function(err) {
        scope.page.message = 'Error: ' + err;
        deferred.reject(scope.page.message);
    });
    return deferred.promise;
}
function fillPageStyle(blocks, minPos, animateAll) {
    computePositions(blocks);
    var id = 0, i;
    for (i in blocks) {
        minPos = checkOutOfScreen(blocks[i], minPos);
    }
    for (i in blocks) {
        fillBlockStyle(blocks[i], id++, minPos, animateAll || blocks[i].animate);
    }
}
function checkOutOfScreen(block, minPos) {
    var marginLeft = -FrontBlock.HALF_WIDTH + block.posx * FrontBlock.WIDTH;
    var marginTop = -FrontBlock.HALF_HEIGHT + block.posy * FrontBlock.HEIGHT;
    var x = window.innerWidth / 2 + marginLeft;
    var y = window.innerHeight / 2 + marginTop;
    if (x < minPos.x) {
        minPos.x = x;
    }
    if (y < minPos.y) {
        minPos.y = y;
    }
    return minPos;
}
var lightBlockColor = function() {
    // With R, G, B: one is 30, one is 60, the third is rnd[30-60]
    var rgb = ["30", "60", "" + (30 + Math.floor(Math.random()*30))];
    // Shuffle rgb
    var j, x, i;
    for (i = 2; i > 0; i--) {
        j = Math.floor(Math.random() * (i + 1));
        x = rgb[i];
        rgb[i] = rgb[j];
        rgb[j] = x;
    }
    return "#" + rgb[0] + rgb[1] + rgb[2];
}();
function fillBlockStyle(block, id, minPos, animate) {
    block.styleData = {
        marginLeft: -minPos.x - FrontBlock.HALF_WIDTH + block.posx * FrontBlock.WIDTH,
        marginTop: -minPos.y - FrontBlock.HALF_HEIGHT + block.posy * FrontBlock.HEIGHT,
        color: ((block.posx + block.posy) % 2) ? lightBlockColor : "#020202",
        dx: 0,
        dy: 0
    };
    block.id = id;
    computeBlockStyle(block, animate);
}
var effectTypes = ["translate", "rotate", "scale", "color"];
var effectType = effectTypes[Math.floor(Math.random()*effectTypes.length)];
function getRandomEffect() {
    var effect = effectType + Math.floor(1 + Math.random()*3);
    var duration = Math.floor((2+Math.random()*20)) / 20.0;
    return "-moz-animation-name: " + effect + "; -moz-animation-iteration-count: 1; -moz-animation-timing-function: ease-in; -moz-animation-duration: " + duration + "s;"
        + "-webkit-animation-name: " + effect + "; -webkit-animation-iteration-count: 1; -webkit-animation-timing-function: ease-in; -webkit-animation-duration: " + duration + "s;"
        + "animation-name: " + effect + "; animation-iteration-count: 1; animation-timing-function: ease-in; animation-duration: " + duration + "s;";
}
function computeBlockStyle(block, animate) {
    var marginLeft = block.styleData.marginLeft + block.styleData.dx;
    var marginTop = block.styleData.marginTop + block.styleData.dy;
    block.style = "margin-left: " + marginLeft + "px; margin-top: " + marginTop + "px; background-color: " + block.styleData.color + "; ";
    if (animate) {
        block.style += getRandomEffect();
    }
    block.NStyle = "margin-left: " + (marginLeft + 100) + "px; margin-top: " + marginTop + "px;";
    block.SStyle = "margin-left: " + (marginLeft + 100) + "px; margin-top: " + (marginTop + 200) + "px;";
    block.EStyle = "margin-left: " + (marginLeft + 200) + "px; margin-top: " + (marginTop + 100) + "px;";
    block.WStyle = "margin-left: " + marginLeft + "px; margin-top: " + (marginTop + 100) + "px;";
    return block.style;
}
function isFreePosition(pos, page) {
    for (var i in page.blocks) {
        var block = page.blocks[i];
        if (pos.x == block.posx && pos.y == block.posy) {
            return false;
        }
    }
    return true;
}
function findFreePosition(page) {
    // Spiral algorithm
    var deep = 0;
    var fRounds = function(d) { return (2*d+1) * (2*d+1); };
    var nRounds = fRounds(deep);
    var t = 0;
    var fpos = function(x,y) { return {x: x, y: y}};
    var pos = fpos(0, 0);
    var ops = [
        function(p) {return fpos(p.x+1, p.y) },
        function(p) {return fpos(p.x, p.y+1) },
        function(p) {return fpos(p.x-1, p.y) },
        function(p) {return fpos(p.x, p.y-1) }];
    var curOp = 0;
    while (!isFreePosition(pos, page)) {
        t++;
        if (t == nRounds) {
            deep++;
            nRounds = fRounds(deep);
        }
        var testPos = ops[curOp](pos);
        if (Math.abs(testPos.x) > deep || Math.abs(testPos.y) > deep) {
            // Invalid operation; increment op cursor
            curOp = (curOp + 1) % 4;
            pos = ops[curOp](pos);
        } else {
            pos = testPos;
        }
        if (t > 999) {
            console.error("WTF you want to kill me?!");
            break;
        }
    }
    return pos;
}
function mergeInPage(page, blocks) {
    for (var i in blocks) {
        var pos = findFreePosition(page);
        blocks[i].posx = pos.x;
        blocks[i].posy = pos.y;
        page.blocks.push(blocks[i]);
    }
}
function enterBlock($scope, block) {
    if (block.type == "image") {
        block.active = 0;
        if (block.links.length > 1) {
            // Slideshow
            setInterval(function () {
                block.active = (block.active + 1) % block.links.length;
                $scope.$apply();
            }, 10000);
        }
    }
}
