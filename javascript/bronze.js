/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/

var inputs = readline().split(' ');
var width = parseInt(inputs[0]);
var height = parseInt(inputs[1]);
var myId = parseInt(inputs[2]);

var me = {};
var map = null;

var BOMB_RANGE = 3;

var goingTo = null;

// game loop
while (true) {
    map = new Array(width);
    for (var i=0; i<width; i++) map[i] = new Array(height);

    for (var i=0; i<height; i++) {
        var row = readline();
        var boxes = row.split("");
        for (var j=0; j<boxes.length; j++) {
            if (boxes[j] >= 0) map[j][i] = {"box": true};
            else if (boxes[j] == 'X') map[j][i] = {"wall": true};
            else map[j][i] = {};
        }
    }
    var entities = parseInt(readline());
    for (var i=0; i<entities; i++) {
        var inputs = readline().split(' ');
        var entityType = parseInt(inputs[0]);
        var owner = parseInt(inputs[1]);
        var x = parseInt(inputs[2]);
        var y = parseInt(inputs[3]);
        var param1 = parseInt(inputs[4]);
        var param2 = parseInt(inputs[5]);

        map[x][y] = {
            player: entityType === 0,
            bomb: entityType === 1,
            item: entityType === 2
        };
        if (map[x][y].item) {
            map[x][y].itemType = param1 === 1 ? 'extraRange':'extraBomb';
        }

        if (map[x][y].bomb) {
            map[x][y].bombTurnsLeft = param1;
            map[x][y].bombRange = param2;
            map[x][y].mine = owner == myId;

            if (map[x][y].mine && map[x][y].bombRange>BOMB_RANGE) BOMB_RANGE = map[x][y].bombRange; //keep my bomb range updated
        }

        if (map[x][y].player && owner==myId) {
            map[x][y].me = true;
            me = {
                x: x,
                y: y
            }
        }
    }

    computeMapScore();
    computeReachability();
    printScoreMap();
    //printMap();

    var action = 'MOVE';
    goingTo = nextMove();

    if (iAmSafe() && isSafeToPlaceBomb() && canDestroySomething()) {
        action = 'BOMB';
        goingTo = findClosestSafeSpot();
    }

    if (!goingTo) {
        print(action + ' ' + me.x + ' ' + me.y + ' FUCK');
    } else {
        print(action + ' ' + goingTo.x + ' ' + goingTo.y);
    }
}

function canDestroySomething() {
    return map[me.x][me.y].score > 0;
}

function nextMove() {
    //sorry, you can't copy/paste this code :P
}

function iAmSafe() {
    return !map[me.x][me.y].bombInRange;
}

function computeMapScore() {
    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            map[x][y].score = calculateCellScore(x,y);
            map[x][y].dist = Math.sqrt(Math.pow(me.x-x, 2) + Math.pow(me.y-y, 2));
            var bombs = inRangeOfBomb(x, y);
            if (bombs.bombInRange) map[x][y].bombInRange = true;
            if (bombs.enemyBombInRange) map[x][y].enemyBombInRange = true;
        }
    }
}

function calculateCellScore(x, y) {
    if (map[x][y].box || map[x][y].wall) return 0;

    var x1 = x - (BOMB_RANGE);
    var y1 = y - (BOMB_RANGE);
    var x2 = x + (BOMB_RANGE);
    var y2 = y + (BOMB_RANGE);
    if (x1 < 0) x1 = 0;
    if (y1 < 0) y1 = 0;
    if (x2 > width) x2 = width;
    if (y2 > height) y2 = height;

    var score = 0;
    for (var i=x+1; i<x2; i++)
        if (isATarget(i,y)) {
            score++;
            break; //bomb only hits the first target
        } else if (isAnObstacle(i,y)) {
            break;
        }

    for (var i=x-1; i>x1; i--)
        if (isATarget(i,y)) {
            score++;
            break; //bomb only hits the first target
        } else if (isAnObstacle(i,y)) {
            break;
        }

    for (var j=y+1; j<y2; j++)
        if (isATarget(x,j)) {
            score++;
            break; //bomb only hits the first target
        } else if (isAnObstacle(x,j)) {
            break;
        }

    for (var j=y-1; j>y1; j--)
        if (isATarget(x,j)) {
            score++;
            break; //bomb only hits the first target
        } else if (isAnObstacle(x,j)) {
            break;
        }

    if (map[x][y].player && !map[x][y].me) score++; //an enemy is just in this coords
    return score;
}

function isATarget(x, y) {
    return map[x][y].box || (map[x][y].player && !map[x][y].me);
}

function isAnObstacle(x, y) {
    return map[x][y].bomb || map[x][y].wall;
}

function isSafeToPlaceBomb() {
    printErr('What would happen if I place a bomb here?');
    map[me.x][me.y].bomb = true;
    map[me.x][me.y].bombRange = BOMB_RANGE;
    map[me.x][me.y].bombsTurnLeft = 7;
    computeMapScore();
    computeReachability();
    printMap();
    var safeSpot = findClosestSafeSpot();
    printErr(JSON.stringify(safeSpot));
    var fastestBomb = findFastestBomb();
    printErr('fastests bomb explodes in ' + fastestBomb.turnsLeft + ' at ' + fastestBomb.x + ', ' + fastestBomb.y);
    return safeSpot != null && safeSpot.steps < fastestBomb.turnsLeft;
}

function findFastestBomb() {
    var fastest = {
        x: 0,
        y: 0,
        turnsLeft: 99
    };
    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            if (map[x][y].bomb && map[x][y].bombTurnsLeft<fastest.turnsLeft) {
                fastest = {
                    x: x,
                    y: y,
                    turnsLeft: map[x][y].bombTurnsLeft
                }
            }
        }
    }
    return fastest;
}

function inRangeOfBomb(x, y) {
    var bombInRange = false;
    var enemyBombInRange = false;

    for (var i=x+1; i<width; i++) {
        if (map[i][y].box || map[i][y].wall) break;
        if (map[i][y].bomb && map[i][y].bombRange >= Math.abs(x - i)) {
            bombInRange = true;
            if (!map[i][y].mine) enemyBombInRange = true;
        }
    }

    for (i=x-1; i>=0; i--) {
        if (map[i][y].box || map[i][y].wall) break;
        if (map[i][y].bomb && map[i][y].bombRange >= Math.abs(x - i)) {
            bombInRange = true;
            if (!map[i][y].mine) enemyBombInRange = true;
        }
    }

    for (i=y+1; i<height; i++) {
        if (map[x][i].box || map[x][i].wall) break;
        if (map[x][i].bomb && map[x][i].bombRange >= Math.abs(y - i)) {
            bombInRange = true;
            if (!map[x][i].mine) enemyBombInRange = true;
        }
    }

    for (i=y-1; i>=0; i--) {
        if (map[x][i].box || map[x][i].wall) break;
        if (map[x][i].bomb && map[x][i].bombRange >= Math.abs(y - i)) {
            bombInRange = true;
            if (!map[x][i].mine) enemyBombInRange = true;
        }
    }

    if (map[x][y].bomb) bombInRange = true;
    if (map[x][y].bomb && !map[x][y].mine) enemyBombInRange = true;

    return {
        bombInRange: bombInRange,
        enemyBombInRange: enemyBombInRange
    };
}

function findClosestSafeSpot() {
    var closest = null;
    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            if (map[x][y].reachable && !map[x][y].bombInRange) {
                var dist = map[x][y].steps;
                if (closest == null || dist < closest.steps) closest = {
                    x: x,
                    y: y,
                    steps: dist
                };
            }
        }
    }

    return closest;
}

function findAnotherSafeSpot() {
    var spots = [];
    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            if (map[x][y].reachable && !map[x][y].bombInRange && map[x][y].steps === 1) {
                spots.push({
                    x: x,
                    y: y,
                    steps: map[x][y].steps
                });
            }
        }
    }

    if (spots.length == 0) return null;

    //try to get closer to some boxes (more points)
    var closestBox = findClosestBox();
    printErr('Closest box: ' + closestBox.x + ', ' + closestBox.y);
    var bestSpot = null;
    for (var i=0; i<spots.length; i++) {
        spots[i].dist = Math.sqrt(Math.pow(closestBox.x-spots[i].x, 2) + Math.pow(closestBox.y-spots[i].y, 2));
        if (bestSpot == null || spots[i].dist < bestSpot.dist) bestSpot = spots[i];
    }
    return bestSpot;
}

function findClosestBox() {
    var box = {
        x: me.x,
        y: me.y,
        dist: 999
    };

    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            if (map[x][y].box && map[x][y].dist < box.dist) {
                box = {
                    x: x,
                    y: y,
                    dist: map[x][y].dist
                };
            }
        }
    }
    return box;
}

function computeReachability() {
    reach(me.x-1, me.y, 1);
    reach(me.x+1, me.y, 1);
    reach(me.x, me.y-1, 1);
    reach(me.x, me.y+1, 1);

    map[me.x][me.y].reachable = true;
    map[me.x][me.y].steps = 0;
}

function reach(x, y, steps) {
    if (x<0 || x>=width) return;
    if (y<0 || y>=height) return;
    if (map[x][y].reachable && map[x][y].steps <= steps) return;

    if (map[x][y].wall || map[x][y].box || map[x][y].bomb) map[x][y].reachable = false;
    else {
        map[x][y].reachable = true;
        map[x][y].steps = steps;
        reach(x-1, y, steps+1);
        reach(x+1, y, steps+1);
        reach(x, y-1, steps+1);
        reach(x, y+1, steps+1);
    }
}

function printMap() {
    for (var y=0; y<height; y++) {
        var line = '';
        for (var x=0; x<width; x++) {
            if (map[x][y].reachable) {
                if (map[x][y].bomb) {
                    line += '*';
                } else if (map[x][y].bombInRange) {
                    line += 'D';
                } else {
                    //line += '_';
                    line += map[x][y].steps<10 ? map[x][y].steps:'_';
                }
            } else {
                line += 'X';
            }
        }
        printErr(line);
    }
}

function printScoreMap() {
    for (var y=0; y<height; y++) {
        var line = '';
        for (var x=0; x<width; x++) {
            if (map[x][y].box) line += 'B';
            else if (map[x][y].wall) line += 'W';
            else line += map[x][y].score;
        }
        printErr(line);
    }
}