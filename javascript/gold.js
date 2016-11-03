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
var players = {};

var BOMB_RANGE = 3;
var REMAINING_BOMBS = 1;

var ignorePlayers = true;

// game loop
while (true) {
    map = new Array(width);
    players = {};

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

            trackBombRange(x, y, owner);
        }

        if (map[x][y].player) {
            map[x][y].playerId = 'player-' + owner;
            players['player-' + owner] = {
                id: owner,
                bombRange: 3
            };

            if (owner==myId) {
                map[x][y].me = true;
                me = {
                    x: x,
                    y: y
                }
            }
        }
    }

    if (noMoreBoxesNorItems()) ignorePlayers = false; //killer mode
    computeMapScore(map);
    computeReachability(map, me);
    //printSafetyMap(map);
    //printErr(JSON.stringify(players));
    //printBombTargets(map);
    //printMap(map);

    var action = 'MOVE';
    var move = nextMove(map);

    printErr('next move: ' + JSON.stringify(move));

    //place a bomb while we run away
    if (move!=null && move.run && canDestroySomething(map, me) && REMAINING_BOMBS>0) {
        var tryMove = moveAfterBomb({x: me.x, y: me.y});
        if (tryMove!=null) {
            REMAINING_BOMBS--;
            action = 'BOMB';
            move = tryMove;
        }
    }

    //place a bomb if it is a score move
    if (move!=null && move.bomb && arrived(move.x, move.y) && REMAINING_BOMBS>0) {
        REMAINING_BOMBS--;
        action = 'BOMB';
        move = move.next;
    }

    if (!move) move = {x: me.x, y: me.y, text: 'OOOPS'};
    move = nextCellTowards(map, move); //move just 1 cell each turn (no more automatic moves thanks :)
    print(action + ' ' + move.x + ' ' + move.y + ' ' + move.text);

    calculatePlayerStatus(map, move);
}

function calculatePlayerStatus(map, move) {
    if (map[move.x][move.y].item) {
        var type = map[move.x][move.y].itemType;
        if (type == 'extraBomb') {
            REMAINING_BOMBS++;
        }
        if (type == 'extraRange') {
            BOMB_RANGE++;
        }
    }

    REMAINING_BOMBS += ownBombsExplodingNextTurn();

    printErr('REMAINING_BOMBS='+REMAINING_BOMBS);
}

function ownBombsExplodingNextTurn() {
    var bombs = 0;
    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            if (map[x][y].bomb && map[x][y].mine && map[x][y].bombTurnsLeft == 1) {
                bombs++;
            }
        }
    }
    return bombs;
}

function nextCellTowards(map, move) {
    var path = calculatePath(map, move);
    if (!path || path.length<=1) return move;

    return {
        x: path[1].x,
        y: path[1].y,
        text: move.text + ' (' + move.x + ',' + move.y +')'
    }
}

function arrived(x, y) {
    return me.x == x && me.y == y;
}

function noMoreBoxesNorItems() {
    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            if (map[x][y].box || map[x][y].item) return false;
        }
    }
    return true;
}

function trackBombRange(x, y, playerId) {
    if (map[x][y].mine && map[x][y].bombRange>BOMB_RANGE) BOMB_RANGE = map[x][y].bombRange; //keep my bomb range updated
    else {
        if (!players['player-' + playerId]) players['player-' + playerId] = {id: playerId};
        players['player-' + playerId].bombRange = map[x][y].bombRange;
    }
}

function compareScoreMoves(one, best) {
    if (one.score <= 0) return best;

    if (one.bombInRange) return best;

    if (best.score <= 0) return one;

    var maxSteps = 10;
    if (REMAINING_BOMBS>0) maxSteps = 2;

    if (one.steps > maxSteps) return best;

    if (one.score>=best.score && one.steps<=best.steps) return one;
    else return best;
}

function compareSafeMoves(one, best) {
    if (one.bombInRange) return best;

    if (best.empty) return one;

    if (!best.item && one.item) return one; //mejor un objeto que nada

    if (best.item && one.item && one.steps<best.steps) return one; //mejor un objeto mas cercano

    if (!best.item) {
        if (one.steps<best.steps && one.score>=best.score) return one;
        else return best;
    }

    return best;
}

/**
 *
 * @param map
 * @param onlySafeMove: true if you only want to find a safe move
 * @returns {*}
 */
function nextMove(map, onlySafeMove) {
    if (onlySafeMove == null) onlySafeMove = false;

    var scoreMove = {score: -1, steps: 99, empty: true};
    var safeMove = {score: 0, steps: 99, empty: true};

    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            if (!map[x][y].reachable) continue;

            var safePath = pathIsSafe(map, x, y);

            if (!onlySafeMove && safePath) {
                var bestMove = compareScoreMoves({
                    x: x,
                    y: y,
                    score: map[x][y].score,
                    steps: map[x][y].steps,
                    bombInRange: map[x][y].bombInRange,
                    bomb: true,
                    text: 'SCORE'
                }, scoreMove);

                if (!bestMove.empty) {
                    bestMove.next = bestMove.next != null ? bestMove.next : moveAfterBomb({x: x, y: y});
                }

                if (bestMove.next != null)
                    scoreMove = bestMove;
            }


            if (safePath) {
                safeMove = compareSafeMoves({
                    x: x,
                    y: y,
                    score: map[x][y].score,
                    steps: map[x][y].steps,
                    bombInRange: map[x][y].bombInRange,
                    item: map[x][y].item,
                    run: true,
                    text: map[x][y].item ? 'ITEM':'RUN'
                }, safeMove);
            }
        }
    }

    if (scoreMove.empty && safeMove.empty) return null;
    if (scoreMove.empty) return safeMove;
    if (REMAINING_BOMBS <= 0) return safeMove;
    return scoreMove;
}

function moveAfterBomb(bomb) {
    var newMap = copyFullMap();

    newMap[bomb.x][bomb.y].bomb = true;
    newMap[bomb.x][bomb.y].bombRange = BOMB_RANGE;
    newMap[bomb.x][bomb.y].bombTurnsLeft = 7;
    enemiesPlaceBombs(newMap);
    computeMapScore(newMap);
    computeReachability(newMap, bomb);

    return nextMove(newMap, true); //"true" cause we only care about safe moves (its the important thing)
}

function computeMapScore(map) {
    adjustBombTurnsLeft(map);
    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            var bombs = calculateSafetyForCell(map, x, y);
            if (bombs.bombInRange) map[x][y].bombInRange = true;
            if (bombs.enemyBombInRange) map[x][y].enemyBombInRange = true;
            if (bombs.safeTurns != null) map[x][y].safeTurns = bombs.safeTurns;
        }
    }
    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            map[x][y].score = calculateCellScore(map, x,y);
        }
    }
}

function adjustBombTurnsLeft(map) {
    var bombs = [];
    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            if (map[x][y].bomb) {
                bombs.push({x: x, y: y, turnsLeft: map[x][y].bombTurnsLeft});
            }
        }
    }

    bombs.sort(function(a,b) {return a.turnsLeft-b.turnsLeft});

    for (var i=0; i<bombs.length; i++) {
        var bomb = map[bombs[i].x][bombs[i].y];
        var min = minTurnsLeftOfBombsInRange(map, bombs[i].x, bombs[i].y);
        if (min != null && min < bomb.bombTurnsLeft) bomb.bombTurnsLeft = min;
    }
}

function minTurnsLeftOfBombsInRange(map, bombX, bombY) {
    var bombs = bombsInRange(map, bombX, bombY);
    if (!bombs || bombs.length == 0) return null;

    var min = null;
    var x,y;
    for (var i=0; i<bombs.length; i++) {
        x = bombs[i].x;
        y = bombs[i].y;
        if (!min || map[x][y].bombTurnsLeft < min) min = map[x][y].bombTurnsLeft;
    }
    return min;
}

/**
 * find all bombs in range of x,y
 * @param map
 * @param x
 * @param y
 * @returns {Array}
 */
function bombsInRange(map, x, y) {
    var bombs = [];

    for (var i=x+1; i<width; i++) {
        if (map[i][y].box || map[i][y].wall) break;
        if (map[i][y].bomb && map[i][y].bombRange >= Math.abs(x - i)) {
            bombs.push({
                x: i,
                y: y
            });
        }
    }

    for (i=x-1; i>=0; i--) {
        if (map[i][y].box || map[i][y].wall) break;
        if (map[i][y].bomb && map[i][y].bombRange >= Math.abs(x - i)) {
            bombs.push({
                x: i,
                y: y
            });
        }
    }

    for (i=y+1; i<height; i++) {
        if (map[x][i].box || map[x][i].wall) break;
        if (map[x][i].bomb && map[x][i].bombRange >= Math.abs(y - i)) {
            bombs.push({
                x: x,
                y: i
            });
        }
    }

    for (i=y-1; i>=0; i--) {
        if (map[x][i].box || map[x][i].wall) break;
        if (map[x][i].bomb && map[x][i].bombRange >= Math.abs(y - i)) {
            bombs.push({
                x: x,
                y: i
            });
        }
    }

    return bombs;
}

function calculateCellScore(map, x, y) {
    if (map[x][y].box || map[x][y].wall || map[x][y].bomb) return 0;

    var left = x - (BOMB_RANGE);
    var top = y - (BOMB_RANGE);
    var right = x + (BOMB_RANGE);
    var bot = y + (BOMB_RANGE);

    var score = 0;
    //to the right
    var i = x+1;
    while (i<width && i<right) {
        if (isATarget(map, i,y)) {
            score++;
            break; //bomb only hits the first target
        } else if (isAnExplosionObstacle(i,y)) {
            break;
        }
        i++;
    }

    //to the left
    i = x-1;
    while (i>0 && i>left) {
        if (isATarget(map, i,y)) {
            score++;
            break; //bomb only hits the first target
        } else if (isAnExplosionObstacle(i,y)) {
            break;
        }
        i--;
    }

    //to the bottom
    var j = y+1;
    while (j<height && j<bot) {
        if (isATarget(map, x,j)) {
            score++;
            break; //bomb only hits the first target
        } else if (isAnExplosionObstacle(x,j)) {
            break;
        }
        j++;
    }

    //to the top
    j = y-1;
    while (j>0 && j>top) {
        if (isATarget(map, x,j)) {
            score++;
            break; //bomb only hits the first target
        } else if (isAnExplosionObstacle(x,j)) {
            break;
        }
        j--;
    }

    if (isATarget(map, x, y)) score++; //an enemy or a box is just in this coords
    return score;
}

function isATarget(map, x, y) {
    var boxToDestroy = map[x][y].box && !map[x][y].bombInRange;
    if (ignorePlayers) return boxToDestroy; //survival mode
    else return boxToDestroy || (map[x][y].player && !map[x][y].me); //killer mode
}

function isAnExplosionObstacle(x, y) {
    return map[x][y].bomb || map[x][y].wall || map[x][y].item;
}

function enemiesPlaceBombs(map) {
    //TODO mejorar performance (quitar fors y usar el players[]
    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            if (map[x][y].player && !map[x][y].me) {
                map[x][y].bomb = true;
                map[x][y].bombRange = players[map[x][y].playerId].bombRange + 1;
            }
        }
    }
}

function calculateSafetyForCell(map, x, y) {
    var bombInRange = false;
    var enemyBombInRange = false;
    var safeTurns = null;

    for (var i=x+1; i<width; i++) {
        if (map[i][y].box || map[i][y].wall) break;
        if (map[i][y].bomb && map[i][y].bombRange >= Math.abs(x - i)) {
            bombInRange = true;
            if (!map[i][y].mine) enemyBombInRange = true;
            if (!safeTurns || map[i][y].bombTurnsLeft < safeTurns) safeTurns = map[i][y].bombTurnsLeft;
        }
    }

    for (i=x-1; i>=0; i--) {
        if (map[i][y].box || map[i][y].wall) break;
        if (map[i][y].bomb && map[i][y].bombRange >= Math.abs(x - i)) {
            bombInRange = true;
            if (!map[i][y].mine) enemyBombInRange = true;
            if (!safeTurns || map[i][y].bombTurnsLeft < safeTurns) safeTurns = map[i][y].bombTurnsLeft;
        }
    }

    for (i=y+1; i<height; i++) {
        if (map[x][i].box || map[x][i].wall) break;
        if (map[x][i].bomb && map[x][i].bombRange >= Math.abs(y - i)) {
            bombInRange = true;
            if (!map[x][i].mine) enemyBombInRange = true;
            if (!safeTurns || map[x][i].bombTurnsLeft < safeTurns) safeTurns = map[x][i].bombTurnsLeft;
        }
    }

    for (i=y-1; i>=0; i--) {
        if (map[x][i].box || map[x][i].wall) break;
        if (map[x][i].bomb && map[x][i].bombRange >= Math.abs(y - i)) {
            bombInRange = true;
            if (!map[x][i].mine) enemyBombInRange = true;
            if (!safeTurns || map[x][i].bombTurnsLeft < safeTurns) safeTurns = map[x][i].bombTurnsLeft;
        }
    }

    if (map[x][y].bomb) bombInRange = true;
    if (map[x][y].bomb && !map[x][y].mine) enemyBombInRange = true;
    if (!safeTurns || map[x][y].bombTurnsLeft < safeTurns) safeTurns = map[x][y].bombTurnsLeft;

    return {
        bombInRange: bombInRange,
        enemyBombInRange: enemyBombInRange,
        safeTurns: safeTurns
    };
}

/**
 * Ensure that path is safe from me to x,y
 * That is, all the positions have safeTurns > steps
 * @param x
 * @param y
 */
function pathIsSafe(map, x, y, print) {
    var path = calculatePath(map, {x:x, y:y});
    for (var i=0; i<path.length; i++) {
        var pos = map[path[i].x][path[i].y];
        path[i].safeTurns = pos.safeTurns;
        path[i].steps = pos.steps;
        if (pos.safeTurns <= (pos.steps+1)) {
            return false;
        }
    }
    if (print) printErr('path to ' + x + ',' + y + ' SAFE: ' + JSON.stringify(path));
    return true;
}

function calculatePath(map, dest) {
    var path = [dest];
    var cell = map[dest.x][dest.y];
    for (var i=0; i<map[dest.x][dest.y].steps; i++) {
        path.unshift({x: cell.from.x, y: cell.from.y});
        cell = map[cell.from.x][cell.from.y];
    }
    return path;
}

function copyMapElements() {
    var newMap = new Array(width);
    for(var x=0; x<width; x++) {
        newMap[x] = new Array(height);
        for (var y=0; y<height; y++) {
            newMap[x][y] = {
                box: map[x][y].box || false,
                bomb: map[x][y].bomb || false,
                wall: map[x][y].wall || false
            }
        }
    }
    return newMap;
}

function copyFullMap() {
    var newMap = new Array(width);
    for(var x=0; x<width; x++) {
        newMap[x] = new Array(height);
        for (var y=0; y<height; y++) {
            newMap[x][y] = {
                box: map[x][y].box || false,
                bomb: map[x][y].bomb || false,
                wall: map[x][y].wall || false,
                player: map[x][y].player || false,
                playerId: map[x][y].playerId || null,
                me: map[x][y].me || false,
                item: map[x][y].item || false,
                itemType: map[x][y].itemType || null,
                bombRange: map[x][y].bombRange,
                bombTurnsLeft: map[x][y].bombTurnsLeft
            }
        }
    }
    return newMap;
}

/**
 * Create a map with calculated distances from point x,y (num steps)
 * @param x
 * @param y
 */
function calculateDistancesFrom(x, y) {
    var newMap = copyMapElements();
    computeReachability(newMap, {x:x, y:y});
    return newMap;
}

function computeReachability(map, from) {
    var previousCell = {x: from.x, y: from.y};
    reach(map, from.x-1, from.y, 1, previousCell);
    reach(map, from.x+1, from.y, 1, previousCell);
    reach(map, from.x, from.y-1, 1, previousCell);
    reach(map, from.x, from.y+1, 1, previousCell);

    map[from.x][from.y].reachable = true;
    map[from.x][from.y].steps = 0;
}

function reach(map, x, y, steps, previousCell) {
    if (x<0 || x>=width) return;
    if (y<0 || y>=height) return;
    if (map[x][y].reachable && map[x][y].steps <= steps) return;

    if (map[x][y].wall || map[x][y].box || map[x][y].bomb) {
        map[x][y].reachable = false;
        map[x][y].steps = steps; //we store the "distance" cause we care about bombs
    }
    else {
        map[x][y].reachable = true;
        map[x][y].steps = steps;
        map[x][y].from = previousCell;
        var thisCell = {x: x, y:y};
        reach(map, x-1, y, steps+1, thisCell);
        reach(map, x+1, y, steps+1, thisCell);
        reach(map, x, y-1, steps+1, thisCell);
        reach(map, x, y+1, steps+1, thisCell);
    }
}

function canDestroySomething(map, where) {
    return map[where.x][where.y].score > 0;
}

function printMap(map) {
    for (var y=0; y<height; y++) {
        var line = '';
        for (var x=0; x<width; x++) {
            if (map[x][y].bomb) {
                line += '*';
            } else if (map[x][y].reachable) {
                if (map[x][y].bombInRange) {
                    line += 'D';
                } else {
                    line += map[x][y].steps<10 ? map[x][y].steps:'_';
                }
            } else {
                line += 'X';
            }
        }
        printErr(line);
    }
}

function printScoreMap(map) {
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

function printSafetyMap(map) {
    for (var y=0; y<height; y++) {
        var line = '';
        for (var x=0; x<width; x++) {
            if (map[x][y].bomb) {
                line += '*';
            } else if (map[x][y].safeTurns>=0) {
                line += map[x][y].safeTurns;
            } else {
                line += '_';
            }
        }
        printErr(line);
    }
}

function printBombTargets(map) {
    for (var y=0; y<height; y++) {
        var line = '';
        for (var x=0; x<width; x++) {
            if (map[x][y].box) {
                if (map[x][y].bombInRange) line += 'X';
                else line += '#';
            }
            else if (map[x][y].wall) line += 'W';
            else if (map[x][y].bomb) line += '*';
            else line += map[x][y].score;
        }
        printErr(line);
    }
}