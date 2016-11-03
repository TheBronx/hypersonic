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

var loopStart = null;
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
    loopStart = new Date(); //dont start the timer until we consume some input (thats how we can get our response time)
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
            map[x][y].mine = map[x][y].mine ? true:(owner == myId); //two bombs same place

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
    computeMapInfo(map, me);
    //printMap(map);
    //printSafetyMap(map);
    //printErr(JSON.stringify(players));
    //printScoreMap(map);
    //printBombTargets(map);
    //printMap(map);

    var action = 'MOVE';
    //var before = new Date();
    var move = nextMove(map);
    //printErr('next move took ' + (new Date() - before) + 'ms');

    //printErr('current move: ' + JSON.stringify(move));
    //if (move && move.bomb) printErr('next move: ' + JSON.stringify(move.next));

    //place a bomb if it is a score move
    if (move!=null && move.bomb && arrived(move.x, move.y) && REMAINING_BOMBS>0) {
        REMAINING_BOMBS--;
        action = 'BOMB';
        move = move.next;
    }

    if (!move) {
        move = desperateMove(map);
        if (!move) move = {x: me.x, y: me.y, text: 'OOOPS'};
    }
    move = nextCellTowards(map, move); //move just 1 cell each turn (no more automatic moves thanks :)
    //printErr('response time: ' + (new Date() - loopStart) + 'ms');
    print(action + ' ' + move.x + ' ' + move.y + ' ' + move.text);

    calculatePlayerStatus(map, move);
}

/**
 * find a place that has the highest safeTurns possible
 * @param map
 */
function desperateMove(map) {
    var currentPlaceSafeTurns = map[me.x][me.y].safeTurns;
    var move = {empty: true, steps:99, safeTurns:0};
    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            if (map[x][y].reachable && map[x][y].safeTurns>move.safeTurns && map[x][y].steps<=currentPlaceSafeTurns) {
                var safePath = pathIsSafe(map, x, y);
                if (safePath) {
                    move = {
                        x: x,
                        y: y,
                        steps: map[x][y].steps,
                        safeTurns: map[x][y].safeTurns,
                        run: true,
                        text: 'SURVIVE'
                    };
                }
            }
        }
    }

    if (move.empty) return null;
    return move;
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

    //printErr('REMAINING_BOMBS='+REMAINING_BOMBS);
}

/**
 * Cell is not a box nor a wall nor a bomb
 * NOTE: cells out of map are considered empty too
 * @param map
 * @param x
 * @param y
 * @returns {boolean}
 */
function isEmptyCell(map, x, y) {
    if (x<0 || x>= width) return true; //out of range, consider it an empty cell
    if (y<0 || y>=height) return true;

    var cell = map[x][y];
    return !(cell.box || cell.wall || cell.bomb);
}

function isAnOpenCorner(map, x, y) {
    return (isEmptyCell(map, x+1, y) && isEmptyCell(map, x-1, y) &&
    isEmptyCell(map, x, y+1) && isEmptyCell(map, x, y-1));
}

function moveToKill(map) {
    //buscar puntos de propagacion de bombas (sin paredes en sus ejes)
    //para cada punto, calcular si tiene enemigos a tiro de bomba (score>0)
    //elegir el punto más cercano a nuestra posicion

    var killerMove = {empty: true, steps: 99};
    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            if (map[x][y].reachable /*&& map[x][y].score>0*/ && isAnOpenCorner(map, x, y) && map[x][y].steps<killerMove.steps) {
                var safePath = pathIsSafe(map, x, y);
                if (safePath) {
                    var nextMove = moveAfterBomb({x: x, y: y});
                    if (nextMove) {
                        //I can go there without dying in the path
                        //and I can get out of it if I place the bomb there
                        killerMove = {
                            x: x,
                            y: y,
                            score: map[x][y].score,
                            steps: map[x][y].steps,
                            bombInRange: map[x][y].bombInRange,
                            bomb: true,
                            text: 'KILL',
                            next: nextMove
                        };
                    }
                }
            }
        }
    }

    if (killerMove.empty) return null;
    return killerMove;
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

    var boxMove = null;
    if (!onlySafeMove && ignorePlayers && REMAINING_BOMBS>0) boxMove = moveToScore(map);

    var killerMove = null;
    if (!onlySafeMove && !ignorePlayers && REMAINING_BOMBS>0) killerMove = moveToKill(map);

    var safeMove = null;
    if (boxMove==null && killerMove==null)
        safeMove = moveToStaySafe(map); //performance: if a box/kill move is found, dont waste time finding a safe move

    if (boxMove) return boxMove;
    if (killerMove) return killerMove;
    return safeMove;
}

function moveToStaySafe(map) {
    var safeMove = {score: 0, steps: 99, empty: true};

    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            if (!map[x][y].reachable) continue;

            var safePath = pathIsSafe(map, x, y);

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

    if (safeMove.empty) return null;
    return safeMove;
}

function moveToScore(map) {
    var scoreMove = {score: -1, steps: 99, empty: true};

    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            if (!map[x][y].reachable) continue;

            var safePath = pathIsSafe(map, x, y);

            if (safePath) {
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
        }
    }

    if (scoreMove.empty) return null;
    return scoreMove;
}

function moveAfterBomb(bomb) {
    var newMap = copyFullMap();

    newMap[bomb.x][bomb.y].bomb = true;
    newMap[bomb.x][bomb.y].bombRange = BOMB_RANGE;
    newMap[bomb.x][bomb.y].bombTurnsLeft = 7;
    enemiesPlaceBombs(newMap);
    computeMapInfo(newMap, bomb);

    return nextMove(newMap, true); //"true" cause we only care about safe moves (its the important thing)
}

function computeMapInfo(map, me) {
    adjustBombTurnsLeft(map);
    computeSafety(map);
    computeScore(map);
    computeReachability(map, me);
}

function computeSafety(map) {
    for (var x=0; x<width; x++) {
        for (var y=0; y<height; y++) {
            var bombs = calculateSafetyForCell(map, x, y);
            if (bombs.bombInRange) map[x][y].bombInRange = true;
            if (bombs.enemyBombInRange) map[x][y].enemyBombInRange = true;
            if (bombs.safeTurns != null) map[x][y].safeTurns = bombs.safeTurns;
            if (bombs.explosions != null) map[x][y].explosions = bombs.explosions;
        }
    }
}

function computeScore(map) {
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
        if (min != null && min < bomb.bombTurnsLeft) {
            bomb.bombTurnsLeft = min; //adjust this bomb turns
            //now adjust affected bombs too
            var bombsAffected = bombsInRange(map, bombs[i].x, bombs[i].y);
            for (var j=0; j<bombsAffected.length; j++) {
                var affectedBombCell = map[bombsAffected[j].x][bombsAffected[j].y];
                if (affectedBombCell.bombTurnsLeft>min) affectedBombCell.bombTurnsLeft = min;
            }
        }
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
        if (map[i][y].bomb && map[i][y].bombRange > Math.abs(x - i)) {
            bombs.push({
                x: i,
                y: y
            });
        }
    }

    for (i=x-1; i>=0; i--) {
        if (map[i][y].box || map[i][y].wall) break;
        if (map[i][y].bomb && map[i][y].bombRange > Math.abs(x - i)) {
            bombs.push({
                x: i,
                y: y
            });
        }
    }

    for (i=y+1; i<height; i++) {
        if (map[x][i].box || map[x][i].wall) break;
        if (map[x][i].bomb && map[x][i].bombRange > Math.abs(y - i)) {
            bombs.push({
                x: x,
                y: i
            });
        }
    }

    for (i=y-1; i>=0; i--) {
        if (map[x][i].box || map[x][i].wall) break;
        if (map[x][i].bomb && map[x][i].bombRange > Math.abs(y - i)) {
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

    var left = x - (BOMB_RANGE-1);
    var top = y - (BOMB_RANGE-1);
    var right = x + (BOMB_RANGE-1);
    var bot = y + (BOMB_RANGE-1);

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
    var explosions = [];

    for (var i=x+1; i<width; i++) {
        if (map[i][y].box || map[i][y].wall) break;
        if (map[i][y].bomb && map[i][y].bombRange > Math.abs(x - i)) {
            bombInRange = true;
            if (!map[i][y].mine) enemyBombInRange = true;
            if (!safeTurns || map[i][y].bombTurnsLeft < safeTurns) safeTurns = map[i][y].bombTurnsLeft;
            explosions.push(map[i][y].bombTurnsLeft - 1); //explodes in X turns. 0 means it will explode in the next turn
        }
    }

    for (i=x-1; i>=0; i--) {
        if (map[i][y].box || map[i][y].wall) break;
        if (map[i][y].bomb && map[i][y].bombRange > Math.abs(x - i)) {
            bombInRange = true;
            if (!map[i][y].mine) enemyBombInRange = true;
            if (!safeTurns || map[i][y].bombTurnsLeft < safeTurns) safeTurns = map[i][y].bombTurnsLeft;
            explosions.push(map[i][y].bombTurnsLeft - 1);
        }
    }

    for (i=y+1; i<height; i++) {
        if (map[x][i].box || map[x][i].wall) break;
        if (map[x][i].bomb && map[x][i].bombRange > Math.abs(y - i)) {
            bombInRange = true;
            if (!map[x][i].mine) enemyBombInRange = true;
            if (!safeTurns || map[x][i].bombTurnsLeft < safeTurns) safeTurns = map[x][i].bombTurnsLeft;
            explosions.push(map[x][i].bombTurnsLeft - 1);
        }
    }

    for (i=y-1; i>=0; i--) {
        if (map[x][i].box || map[x][i].wall) break;
        if (map[x][i].bomb && map[x][i].bombRange > Math.abs(y - i)) {
            bombInRange = true;
            if (!map[x][i].mine) enemyBombInRange = true;
            if (!safeTurns || map[x][i].bombTurnsLeft < safeTurns) safeTurns = map[x][i].bombTurnsLeft;
            explosions.push(map[x][i].bombTurnsLeft - 1);
        }
    }

    if (map[x][y].bomb) bombInRange = true;
    if (map[x][y].bomb && !map[x][y].mine) enemyBombInRange = true;
    if (!safeTurns || map[x][y].bombTurnsLeft < safeTurns) safeTurns = map[x][y].bombTurnsLeft;
    explosions.push(map[x][y].bombTurnsLeft - 1);

    return {
        bombInRange: bombInRange,
        enemyBombInRange: enemyBombInRange,
        safeTurns: safeTurns != null ? safeTurns-1 : null, // safeTurns-1 cause we are currently in one of those safe turns
        explosions: explosions
    };
}

/**
 * Ensure that path is safe from me to x,y
 * @param map
 * @param x
 * @param y
 * @param print print safe path
 * @returns {boolean}
 */
function pathIsSafe(map, x, y, print) {
    var path = calculatePath(map, {x:x, y:y});
    for (var i=0; i<path.length; i++) {
        var pos = map[path[i].x][path[i].y];
        path[i].safeTurns = pos.safeTurns;
        path[i].steps = pos.steps;
        if (!pos.bombInRange) continue; //this position is completely safe

        //if the cell explodes when we are in it, its not very safe...
        if (pos.explosions.indexOf(pos.steps) >= 0) return false; //this cell will explode when we reach it!

        //if there is a player there, and places bomb, in 3 turns it will explode. it will be too late to scape!
        //if (pos.player && !pos.me && pos.safeTurns<=3) return false;
        //esto da problemas a la hora de escapar cuando coincides con un jugador en una casilla y pone una bomba
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
    printErr('normal map');
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
    printErr('score map');
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
    printErr('safety map');
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