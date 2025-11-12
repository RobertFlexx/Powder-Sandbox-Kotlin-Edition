import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

// ===== Minimal ncurses binding via JNA =====
object Curses {
    interface Lib : Library {
        fun initscr(): Pointer
        fun endwin(): Int
        fun cbreak()
        fun noecho()
        fun curs_set(visibility: Int): Int
        fun keypad(win: Pointer, bf: Boolean)
        fun nodelay(win: Pointer, bf: Boolean)
        fun has_colors(): Boolean
        fun start_color(): Int
        fun use_default_colors(): Int
        fun init_pair(pair: Short, f: Short, b: Short): Int
        fun attron(attr: Int): Int
        fun attroff(attr: Int): Int
        fun mvaddch(y: Int, x: Int, ch: Int): Int
        fun mvaddnstr(y: Int, x: Int, str: String, n: Int): Int
        fun mvhline(y: Int, x: Int, ch: Int, n: Int): Int
        fun getmaxy(win: Pointer): Int
        fun getmaxx(win: Pointer): Int
        fun newwin(nlines: Int, ncols: Int, beginY: Int, beginX: Int): Pointer
        fun box(win: Pointer, verch: Int, horch: Int): Int
        fun mvwaddnstr(win: Pointer, y: Int, x: Int, str: String, n: Int): Int
        fun wrefresh(win: Pointer): Int
        fun delwin(win: Pointer): Int
        fun flushinp()
        fun getch(): Int
        fun wgetch(win: Pointer): Int
        fun erase(): Int
        fun refresh(): Int
        fun napms(ms: Int): Int
    }

    // if your system only has "ncursesw", change "ncurses" -> "ncursesw"
    val lib: Lib = Native.load("ncurses", Lib::class.java)
}

lateinit var stdscr: Pointer

const val ERR = -1
const val KEY_LEFT = 260
const val KEY_RIGHT = 261
const val KEY_UP = 259
const val KEY_DOWN = 258
const val KEY_ENTER = 10

// curses color constants as Ints (we cast to short when calling C)
const val COLOR_BLACK = 0
const val COLOR_RED = 1
const val COLOR_GREEN = 2
const val COLOR_YELLOW = 3
const val COLOR_BLUE = 4
const val COLOR_MAGENTA = 5
const val COLOR_CYAN = 6
const val COLOR_WHITE = 7

// A_REVERSE bit (typical value)
const val A_REVERSE = 0x0200

fun COLOR_PAIR(n: Short): Int = (n.toInt() shl 8)

// Thin wrappers so main code looks C++-ish
fun initscr() {
    stdscr = Curses.lib.initscr()
}
fun endwin(): Int = Curses.lib.endwin()
fun cbreak() = Curses.lib.cbreak()
fun noecho() = Curses.lib.noecho()
fun curs_set(v: Int): Int = Curses.lib.curs_set(v)
fun keypad(win: Pointer, bf: Boolean) = Curses.lib.keypad(win, bf)
fun nodelay(win: Pointer, bf: Boolean) = Curses.lib.nodelay(win, bf)
fun has_colors(): Boolean = Curses.lib.has_colors()
fun start_color(): Int = Curses.lib.start_color()
fun use_default_colors(): Int = Curses.lib.use_default_colors()

// wrapper to keep calls identical to C++: init_pair(1, COLOR_BLACK, -1);
fun init_pair(pair: Int, f: Int, b: Int): Int =
    Curses.lib.init_pair(pair.toShort(), f.toShort(), b.toShort())

fun attron(attr: Int): Int = Curses.lib.attron(attr)
fun attroff(attr: Int): Int = Curses.lib.attroff(attr)
fun mvaddch(y: Int, x: Int, ch: Char): Int = Curses.lib.mvaddch(y, x, ch.toInt())
fun mvaddch(y: Int, x: Int, ch: Int): Int = Curses.lib.mvaddch(y, x, ch)
fun mvaddnstr(y: Int, x: Int, str: String, n: Int): Int = Curses.lib.mvaddnstr(y, x, str, n)
fun mvhline(y: Int, x: Int, ch: Char, n: Int): Int = Curses.lib.mvhline(y, x, ch.toInt(), n)

// our own wrapper using real ncurses functions getmaxy/getmaxx
fun getmaxyx(win: Pointer): Pair<Int, Int> {
    val y = Curses.lib.getmaxy(win)
    val x = Curses.lib.getmaxx(win)
    return y to x
}

fun newwin(h: Int, w: Int, y: Int, x: Int): Pointer = Curses.lib.newwin(h, w, y, x)
fun box(win: Pointer, verch: Char, horch: Char): Int =
    Curses.lib.box(win, verch.toInt(), horch.toInt())
fun mvwaddnstr(win: Pointer, y: Int, x: Int, str: String, n: Int): Int =
    Curses.lib.mvwaddnstr(win, y, x, str, n)

fun wrefresh(win: Pointer): Int = Curses.lib.wrefresh(win)
fun delwin(win: Pointer): Int = Curses.lib.delwin(win)
fun flushinp() = Curses.lib.flushinp()
fun getch(): Int = Curses.lib.getch()
fun wgetch(win: Pointer): Int = Curses.lib.wgetch(win)
fun erase(): Int = Curses.lib.erase()
fun refresh(): Int = Curses.lib.refresh()
fun napms(ms: Int): Int = Curses.lib.napms(ms)

// ===== Elements =====
enum class Element {
    EMPTY,
    // powders
    SAND, GUNPOWDER, ASH, SNOW,
    // liquids
    WATER, SALTWATER, OIL, ETHANOL, ACID, LAVA, MERCURY,
    // solids / terrain
    STONE, GLASS, WALL, WOOD, PLANT, METAL, WIRE, ICE, COAL,
    DIRT, WET_DIRT, SEAWEED,
    // gases
    SMOKE, STEAM, GAS, TOXIC_GAS, HYDROGEN, CHLORINE,
    // actors / special
    FIRE, LIGHTNING, HUMAN, ZOMBIE
}

data class Cell(
    var type: Element = Element.EMPTY,
    var life: Int = 0    // age / gas lifetime / charge / wetness / anim tick
)

var gWidth = 0
var gHeight = 0
lateinit var grid: Array<Array<Cell>>

private val rng = Random(System.nanoTime())

private fun in_bounds(x: Int, y: Int): Boolean =
    x >= 0 && x < gWidth && y >= 0 && y < gHeight

private fun rint(a: Int, b: Int): Int =
    a + rng.nextInt(b - a + 1)

private fun chance(p: Int): Boolean =
    (rng.nextInt(100) + 1) <= p

private fun empty(c: Cell): Boolean = c.type == Element.EMPTY

// classification helpers
private fun sandlike(e: Element): Boolean =
    e == Element.SAND || e == Element.GUNPOWDER || e == Element.ASH || e == Element.SNOW

private fun liquid(e: Element): Boolean =
    when (e) {
        Element.WATER, Element.SALTWATER, Element.OIL,
        Element.ETHANOL, Element.ACID, Element.LAVA,
        Element.MERCURY -> true
        else -> false
    }

private fun solid(e: Element): Boolean =
    when (e) {
        Element.STONE, Element.GLASS, Element.WALL, Element.WOOD,
        Element.PLANT, Element.METAL, Element.WIRE, Element.ICE,
        Element.COAL, Element.DIRT, Element.WET_DIRT, Element.SEAWEED -> true
        else -> false
    }

private fun gas(e: Element): Boolean =
    when (e) {
        Element.SMOKE, Element.STEAM, Element.GAS,
        Element.TOXIC_GAS, Element.HYDROGEN, Element.CHLORINE -> true
        else -> false
    }

private fun flammable(e: Element): Boolean =
    e == Element.WOOD || e == Element.PLANT || e == Element.OIL ||
        e == Element.ETHANOL || e == Element.GUNPOWDER || e == Element.COAL ||
        e == Element.SEAWEED

private fun conductor(e: Element): Boolean =
    e == Element.METAL || e == Element.WIRE || e == Element.MERCURY || e == Element.SALTWATER

private fun dissolvable(e: Element): Boolean =
    e == Element.SAND || e == Element.STONE || e == Element.GLASS ||
        e == Element.WOOD || e == Element.PLANT || e == Element.METAL ||
        e == Element.WIRE || e == Element.ASH || e == Element.COAL ||
        e == Element.SEAWEED || e == Element.DIRT || e == Element.WET_DIRT

// relative density
private fun density(e: Element): Int =
    when (e) {
        Element.ETHANOL -> 85
        Element.OIL -> 90
        Element.GAS, Element.HYDROGEN -> 1
        Element.STEAM -> 2
        Element.SMOKE -> 3
        Element.CHLORINE -> 5
        Element.WATER -> 100
        Element.SALTWATER -> 103
        Element.ACID -> 110
        Element.LAVA -> 160
        Element.MERCURY -> 200
        else -> 999
    }

// harmful stuff for humans/zombies
private fun is_hazard(e: Element): Boolean =
    e == Element.FIRE || e == Element.LAVA || e == Element.ACID ||
        e == Element.TOXIC_GAS || e == Element.CHLORINE || e == Element.LIGHTNING

private fun name_of(e: Element): String =
    when (e) {
        Element.EMPTY -> "Empty"
        Element.SAND -> "Sand"
        Element.GUNPOWDER -> "Gunpowder"
        Element.ASH -> "Ash"
        Element.SNOW -> "Snow"
        Element.WATER -> "Water"
        Element.SALTWATER -> "Salt Water"
        Element.OIL -> "Oil"
        Element.ETHANOL -> "Ethanol"
        Element.ACID -> "Acid"
        Element.LAVA -> "Lava"
        Element.MERCURY -> "Mercury"
        Element.STONE -> "Stone"
        Element.GLASS -> "Glass"
        Element.WALL -> "Wall"
        Element.WOOD -> "Wood"
        Element.PLANT -> "Plant"
        Element.METAL -> "Metal"
        Element.WIRE -> "Wire"
        Element.ICE -> "Ice"
        Element.COAL -> "Coal"
        Element.DIRT -> "Dirt"
        Element.WET_DIRT -> "Wet Dirt"
        Element.SEAWEED -> "Seaweed"
        Element.SMOKE -> "Smoke"
        Element.STEAM -> "Steam"
        Element.GAS -> "Gas"
        Element.TOXIC_GAS -> "Toxic Gas"
        Element.HYDROGEN -> "Hydrogen"
        Element.CHLORINE -> "Chlorine"
        Element.FIRE -> "Fire"
        Element.LIGHTNING -> "Lightning"
        Element.HUMAN -> "Human"
        Element.ZOMBIE -> "Zombie"
    }

private fun color_of(e: Element): Short =
    when (e) {
        Element.EMPTY -> 1
        // yellow-ish
        Element.SAND,
        Element.GUNPOWDER,
        Element.SNOW,
        Element.DIRT -> 2
        // cyan water-ish
        Element.WATER,
        Element.SALTWATER,
        Element.STEAM,
        Element.ICE,
        Element.ETHANOL -> 3
        // white solids
        Element.STONE,
        Element.GLASS,
        Element.WALL,
        Element.METAL,
        Element.WIRE,
        Element.COAL,
        Element.WET_DIRT -> 4
        // green stuff & humans
        Element.WOOD,
        Element.PLANT,
        Element.SEAWEED,
        Element.HUMAN -> 5
        // red danger
        Element.FIRE,
        Element.LAVA,
        Element.ZOMBIE -> 6
        // magenta haze
        Element.SMOKE,
        Element.ASH,
        Element.GAS,
        Element.HYDROGEN -> 7
        // blue heavy liquids
        Element.OIL,
        Element.MERCURY -> 8
        // green/yellow chem/bolt
        Element.ACID,
        Element.TOXIC_GAS,
        Element.CHLORINE,
        Element.LIGHTNING -> 9
    }

private fun glyph_of(e: Element): Char =
    when (e) {
        Element.EMPTY -> ' '
        Element.SAND -> '.'
        Element.GUNPOWDER -> '%'
        Element.ASH -> ';'
        Element.SNOW -> ','
        Element.WATER -> '~'
        Element.SALTWATER -> ':'
        Element.OIL -> 'o'
        Element.ETHANOL -> 'e'
        Element.ACID -> 'a'
        Element.LAVA -> 'L'
        Element.MERCURY -> 'm'
        Element.STONE -> '#'
        Element.GLASS -> '='
        Element.WALL -> '@'
        Element.WOOD -> 'w'
        Element.PLANT -> 'p'
        Element.SEAWEED -> 'v'
        Element.METAL -> 'M'
        Element.WIRE -> '-'
        Element.ICE -> 'I'
        Element.COAL -> 'c'
        Element.DIRT -> 'd'
        Element.WET_DIRT -> 'D'
        Element.SMOKE -> '^'
        Element.STEAM -> '"'
        Element.GAS -> '`'
        Element.TOXIC_GAS -> 'x'
        Element.HYDROGEN -> '\''
        Element.CHLORINE -> 'X'
        Element.FIRE -> '*'
        Element.LIGHTNING -> '|'
        Element.HUMAN -> 'Y'
        Element.ZOMBIE -> 'T'
    }

// ===== Grid =====
private fun init_grid(w: Int, h: Int) {
    gWidth = w
    gHeight = h
    grid = Array(gHeight) { Array(gWidth) { Cell() } }
}

private fun clear_grid() {
    for (y in 0 until gHeight) {
        for (x in 0 until gWidth) {
            val c = grid[y][x]
            c.type = Element.EMPTY
            c.life = 0
        }
    }
}

// swap contents (by value)
private fun swapCells(a: Cell, b: Cell) {
    val tType = a.type
    val tLife = a.life
    a.type = b.type
    a.life = b.life
    b.type = tType
    b.life = tLife
}

// ===== Helpers =====
private fun explode(cx: Int, cy: Int, r: Int) {
    for (dy in -r..r) {
        for (dx in -r..r) {
            val x = cx + dx
            val y = cy + dy
            if (!in_bounds(x, y)) continue
            if (dx * dx + dy * dy > r * r) continue
            val c = grid[y][x]
            if (c.type == Element.WALL) continue
            if (c.type == Element.STONE || c.type == Element.GLASS ||
                c.type == Element.METAL || c.type == Element.WIRE ||
                c.type == Element.ICE
            ) continue

            val roll = rint(1, 100)
            if (roll <= 50) {
                c.type = Element.FIRE
                c.life = 15 + rint(0, 10)
            } else if (roll <= 80) {
                c.type = Element.SMOKE
                c.life = 20
            } else {
                c.type = Element.GAS
                c.life = 20
            }
        }
    }
}

private fun place_brush(cx: Int, cy: Int, rad: Int, e: Element) {
    if (e == Element.LIGHTNING) {
        if (!in_bounds(cx, cy)) return
        val x = cx
        var y = cy
        while (y + 1 < gHeight) {
            val below = grid[y + 1][x]
            if (!empty(below) && !gas(below.type)) break
            y++
        }
        for (yy in cy..y) {
            val c = grid[yy][x]
            c.type = Element.LIGHTNING
            c.life = 2
        }
        if (y + 1 < gHeight) {
            val below = grid[y + 1][x]
            if (below.type == Element.WATER || below.type == Element.SALTWATER) {
                below.life = max(below.life, 8)
            }
        }
        return
    }

    val r2 = rad * rad
    for (dy in -rad..rad) {
        for (dx in -rad..rad) {
            val x = cx + dx
            val y = cy + dy
            if (!in_bounds(x, y)) continue
            if (dx * dx + dy * dy <= r2) {
                val c = grid[y][x]
                c.type = e
                c.life = 0
                if (gas(e)) c.life = 25
                if (e == Element.FIRE) c.life = 20
            }
        }
    }
}

// ===== Simulation =====
private fun step_sim() {
    if (gWidth <= 0 || gHeight <= 0) return
    val updated = Array(gHeight) { BooleanArray(gWidth) }

    for (y in gHeight - 1 downTo 0) {
        for (x in 0 until gWidth) {
            if (updated[y][x]) continue
            val cell = grid[y][x]
            val t = cell.type
            if (t == Element.EMPTY || t == Element.WALL) {
                updated[y][x] = true
                continue
            }

            val swap_to: (Int, Int) -> Unit = { nx, ny ->
                swapCells(grid[ny][nx], cell)
                updated[ny][nx] = true
            }

            // --- powders ---
            if (sandlike(t)) {
                var moved = false

                if (in_bounds(x, y + 1)) {
                    val below = grid[y + 1][x]
                    if (empty(below) || liquid(below.type)) {
                        swap_to(x, y + 1)
                        moved = true
                    }
                }
                if (!moved) {
                    val dir = if (rint(0, 1) == 1) 1 else -1
                    for (i in 0 until 2) {
                        if (moved) break
                        val nx = x + if (i != 0) -dir else dir
                        val ny = y + 1
                        if (!in_bounds(nx, ny)) continue
                        val d = grid[ny][nx]
                        if (empty(d) || liquid(d.type)) {
                            swap_to(nx, ny)
                            moved = true
                        }
                    }
                }
                if (!moved) updated[y][x] = true

                if (t == Element.SNOW) {
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (!in_bounds(nx, ny)) continue
                            val ne = grid[ny][nx].type
                            if (ne == Element.FIRE || ne == Element.LAVA) {
                                cell.type = Element.WATER
                                cell.life = 0
                            }
                        }
                    }
                }

                if (t == Element.SAND) {
                    if (in_bounds(x, y - 1) && grid[y - 1][x].type == Element.WATER) {
                        cell.life++
                        if (cell.life > 220) {
                            var nearbyWeed = false
                            for (wy in -2..2) {
                                if (nearbyWeed) break
                                for (wx in -2..2) {
                                    val sx = x + wx
                                    val sy = y + wy
                                    if (!in_bounds(sx, sy)) continue
                                    if (grid[sy][sx].type == Element.SEAWEED) {
                                        nearbyWeed = true
                                        break
                                    }
                                }
                            }
                            if (!nearbyWeed && in_bounds(x, y - 1) &&
                                grid[y - 1][x].type == Element.WATER
                            ) {
                                grid[y - 1][x].type = Element.SEAWEED
                                grid[y - 1][x].life = 0
                            }
                            cell.life = 0
                        }
                    } else {
                        cell.life = 0
                    }
                }

                continue
            }

            // --- liquids ---
            if (liquid(t)) {
                var moved = false

                if (in_bounds(x, y + 1)) {
                    val b = grid[y + 1][x]
                    if (empty(b) || gas(b.type)) {
                        swap_to(x, y + 1)
                        moved = true
                    } else if (liquid(b.type) && density(t) > density(b.type)) {
                        swap_to(x, y + 1)
                        moved = true
                    }
                }

                if (!moved) {
                    val order = intArrayOf(-1, 1)
                    if (rint(0, 1) == 1) {
                        val tmp = order[0]
                        order[0] = order[1]
                        order[1] = tmp
                    }
                    for (i in 0 until 2) {
                        if (moved) break
                        val nx = x + order[i]
                        if (!in_bounds(nx, y)) continue
                        val s = grid[y][nx]
                        if (empty(s) || gas(s.type)) {
                            swap_to(nx, y)
                            moved = true
                        } else if (liquid(s.type) && density(t) > density(s.type) && chance(50)) {
                            swap_to(nx, y)
                            moved = true
                        }
                    }
                }

                if (!moved) updated[y][x] = true

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (!in_bounds(nx, ny)) continue
                        val n = grid[ny][nx]

                        if (t == Element.WATER || t == Element.SALTWATER) {
                            if (n.type == Element.FIRE) {
                                n.type = Element.SMOKE
                                n.life = 15
                            } else if (n.type == Element.LAVA) {
                                n.type = Element.STONE
                                n.life = 0
                                if (chance(50)) {
                                    cell.type = Element.STEAM
                                    cell.life = 20
                                } else {
                                    cell.type = Element.STONE
                                    cell.life = 0
                                }
                            }
                        }

                        if (t == Element.OIL || t == Element.ETHANOL) {
                            if (n.type == Element.FIRE || n.type == Element.LAVA) {
                                cell.type = Element.FIRE
                                cell.life = 25
                            }
                        }

                        if (t == Element.ACID) {
                            if (dissolvable(n.type)) {
                                if (chance(30)) {
                                    n.type = Element.TOXIC_GAS
                                    n.life = 25
                                } else {
                                    n.type = Element.EMPTY
                                    n.life = 0
                                }
                                if (chance(25)) {
                                    cell.type = Element.EMPTY
                                    cell.life = 0
                                }
                            }
                            if (n.type == Element.WATER && chance(30)) {
                                cell.type = Element.SALTWATER
                                cell.life = 0
                                if (chance(30)) {
                                    n.type = Element.STEAM
                                    n.life = 20
                                }
                            }
                        }

                        if (t == Element.LAVA) {
                            if (flammable(n.type)) {
                                n.type = Element.FIRE
                                n.life = 25
                            } else if (n.type == Element.SAND || n.type == Element.SNOW) {
                                n.type = Element.GLASS
                                n.life = 0
                            } else if (n.type == Element.WATER || n.type == Element.SALTWATER) {
                                n.type = Element.STONE
                                n.life = 0
                                if (chance(50)) {
                                    cell.type = Element.STEAM
                                    cell.life = 20
                                } else {
                                    cell.type = Element.STONE
                                    cell.life = 0
                                }
                            } else if (n.type == Element.ICE) {
                                n.type = Element.WATER
                                n.life = 0
                            }
                        }
                    }
                }

                if (t == Element.LAVA) {
                    cell.life++
                    if (cell.life > 200) {
                        cell.type = Element.STONE
                        cell.life = 0
                    }
                }

                if (t == Element.WATER || t == Element.SALTWATER) {
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (!in_bounds(nx, ny)) continue
                            val n = grid[ny][nx]
                            if (n.type == Element.DIRT || n.type == Element.WET_DIRT) {
                                n.type = Element.WET_DIRT
                                n.life = 300
                            }
                        }
                    }
                }

                if ((t == Element.WATER || t == Element.SALTWATER) && cell.life > 0) {
                    val q = cell.life
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = x + dx
                            val ny = y + dy
                            if (!in_bounds(nx, ny)) continue
                            val n = grid[ny][nx]
                            if (n.type == Element.WATER || n.type == Element.SALTWATER) {
                                if (n.life < q - 1) n.life = q - 1
                            }
                            if (n.type == Element.HUMAN || n.type == Element.ZOMBIE) {
                                n.type = Element.ASH
                                n.life = 0
                            }
                        }
                    }
                    cell.life--
                    if (cell.life < 0) cell.life = 0
                }

                continue
            }

            // --- gases ---
            if (gas(t)) {
                var moved = false

                val tries = if (t == Element.HYDROGEN) 2 else 1
                for (i in 0 until tries) {
                    if (moved) break
                    if (in_bounds(x, y - 1) && empty(grid[y - 1][x])) {
                        swap_to(x, y - 1)
                        moved = true
                    }
                }

                if (!moved) {
                    val order = intArrayOf(-1, 1)
                    if (rint(0, 1) == 1) {
                        val tmp = order[0]
                        order[0] = order[1]
                        order[1] = tmp
                    }
                    for (i in 0 until 2) {
                        if (moved) break
                        val nx = x + order[i]
                        val ny = y - if (chance(50)) 1 else 0
                        if (in_bounds(nx, ny) && empty(grid[ny][nx])) {
                            swap_to(nx, ny)
                            moved = true
                        }
                    }
                }

                if (t == Element.HYDROGEN || t == Element.GAS) {
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = x + dx
                            val ny = y + dy
                            if (!in_bounds(nx, ny)) continue
                            val ne = grid[ny][nx].type
                            if (ne == Element.FIRE || ne == Element.LAVA) {
                                if (t == Element.HYDROGEN) {
                                    explode(x, y, 4)
                                } else {
                                    cell.type = Element.FIRE
                                    cell.life = 12
                                }
                            }
                        }
                    }
                }

                if (t == Element.CHLORINE) {
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val nx = x + dx
                            val ny = y + dy
                            if (!in_bounds(nx, ny)) continue
                            if (grid[ny][nx].type == Element.PLANT && chance(35)) {
                                grid[ny][nx].type = Element.TOXIC_GAS
                                grid[ny][nx].life = 25
                            }
                        }
                    }
                }

                cell.life--
                if (cell.life <= 0) {
                    if (t == Element.STEAM && chance(15)) {
                        cell.type = Element.WATER
                        cell.life = 0
                    } else if (t == Element.SMOKE && chance(8)) {
                        cell.type = Element.ASH
                        cell.life = 0
                    } else {
                        cell.type = Element.EMPTY
                        cell.life = 0
                    }
                } else {
                    if (!moved) updated[y][x] = true
                }
                continue
            }

            // --- fire ---
            if (t == Element.FIRE) {
                if (in_bounds(x, y - 1) && (empty(grid[y - 1][x]) || gas(grid[y - 1][x].type)) && chance(50)) {
                    swap_to(x, y - 1)
                }

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (!in_bounds(nx, ny)) continue
                        val n = grid[ny][nx]

                        if (flammable(n.type) && chance(40)) {
                            if (n.type == Element.GUNPOWDER) explode(nx, ny, 5)
                            else {
                                n.type = Element.FIRE
                                n.life = 15 + rint(0, 10)
                            }
                        }
                        if (n.type == Element.WATER || n.type == Element.SALTWATER) {
                            cell.type = Element.SMOKE
                            cell.life = 15
                        }
                        if (n.type == Element.WIRE || n.type == Element.METAL) {
                            if (chance(5)) n.life = max(n.life, 5)
                        }
                    }
                }

                cell.life--
                if (cell.life <= 0) {
                    cell.type = Element.SMOKE
                    cell.life = 15
                }
                updated[y][x] = true
                continue
            }

            // --- lightning ---
            if (t == Element.LIGHTNING) {
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (!in_bounds(nx, ny)) continue
                        val n = grid[ny][nx]
                        val ne = n.type
                        if (ne == Element.WIRE || ne == Element.METAL) {
                            n.life = max(n.life, 12)
                        }
                        if (ne == Element.WATER || ne == Element.SALTWATER) {
                            n.life = max(n.life, 8)
                        }
                        if (flammable(ne)) {
                            if (ne == Element.GUNPOWDER) explode(nx, ny, 6)
                            else {
                                n.type = Element.FIRE
                                n.life = 20 + rint(0, 10)
                            }
                        }
                        if (ne == Element.HYDROGEN || ne == Element.GAS) {
                            explode(nx, ny, 4)
                        }
                    }
                }
                cell.life--
                if (cell.life <= 0) {
                    cell.type = Element.EMPTY
                    cell.life = 0
                }
                updated[y][x] = true
                continue
            }

            val walk_try: (Int, Int) -> Boolean = { tx, ty ->
                if (!in_bounds(tx, ty)) false
                else {
                    val d = grid[ty][tx]
                    if (empty(d) || gas(d.type)) {
                        swapCells(d, cell)
                        true
                    } else false
                }
            }

            // --- HUMAN ---
            if (t == Element.HUMAN) {
                var killed = false
                for (dy in -1..1) {
                    if (killed) break
                    for (dx in -1..1) {
                        if (killed) break
                        val nx = x + dx
                        val ny = y + dy
                        if (!in_bounds(nx, ny)) continue
                        val ne = grid[ny][nx].type
                        if (is_hazard(ne) ||
                            ((ne == Element.WATER || ne == Element.SALTWATER) && grid[ny][nx].life > 0)
                        ) {
                            cell.type = Element.ASH
                            cell.life = 0
                            killed = true
                        }
                    }
                }
                if (killed) {
                    updated[y][x] = true
                    continue
                }

                cell.life++

                if (in_bounds(x, y + 1)) {
                    val b = grid[y + 1][x].type
                    if (empty(grid[y + 1][x]) || gas(b)) {
                        swap_to(x, y + 1)
                        continue
                    }
                }

                var zx = 0
                var zy = 0
                var seen = false
                run {
                    for (ry in -6..6) {
                        if (seen) break
                        for (rx in -6..6) {
                            val nx = x + rx
                            val ny = y + ry
                            if (!in_bounds(nx, ny)) continue
                            if (grid[ny][nx].type == Element.ZOMBIE) {
                                zx = nx
                                zy = ny
                                seen = true
                                break
                            }
                        }
                    }
                }

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (!in_bounds(nx, ny)) continue
                        if (grid[ny][nx].type == Element.ZOMBIE && chance(35)) {
                            if (chance(60)) {
                                grid[ny][nx].type = Element.FIRE
                                grid[ny][nx].life = 10 + rint(0, 10)
                            } else {
                                grid[ny][nx].type = Element.ASH
                                grid[ny][nx].life = 0
                            }
                        }
                    }
                }

                var dir = if (rint(0, 1) == 1) 1 else -1
                if (seen) {
                    dir = if (zx < x) 1 else -1
                }

                if (!walk_try(x + dir, y)) {
                    if (in_bounds(x + dir, y - 1) &&
                        empty(grid[y - 1][x + dir]) &&
                        empty(grid[y - 1][x]) &&
                        chance(70)
                    ) {
                        swapCells(grid[y - 1][x], cell)
                    } else {
                        walk_try(x + if (rint(0, 1) == 1) 1 else -1, y)
                    }
                }

                updated[y][x] = true
                continue
            }

            // --- ZOMBIE ---
            if (t == Element.ZOMBIE) {
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (!in_bounds(nx, ny)) continue
                        val ne = grid[ny][nx].type
                        if (is_hazard(ne) ||
                            ((ne == Element.WATER || ne == Element.SALTWATER) && grid[ny][nx].life > 0)
                        ) {
                            cell.type = Element.FIRE
                            cell.life = 15
                        }
                    }
                }
                if (cell.type != Element.ZOMBIE) {
                    updated[y][x] = true
                    continue
                }

                cell.life++

                if (in_bounds(x, y + 1)) {
                    val b = grid[y + 1][x].type
                    if (empty(grid[y + 1][x]) || gas(b)) {
                        swap_to(x, y + 1)
                        continue
                    }
                }

                var hx = 0
                var hy = 0
                var seen = false
                run {
                    for (ry in -6..6) {
                        if (seen) break
                        for (rx in -6..6) {
                            val nx = x + rx
                            val ny = y + ry
                            if (!in_bounds(nx, ny)) continue
                            if (grid[ny][nx].type == Element.HUMAN) {
                                hx = nx
                                hy = ny
                                seen = true
                                break
                            }
                        }
                    }
                }

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (!in_bounds(nx, ny)) continue
                        if (grid[ny][nx].type == Element.HUMAN) {
                            if (chance(70)) {
                                grid[ny][nx].type = Element.ZOMBIE
                                grid[ny][nx].life = 0
                            } else {
                                grid[ny][nx].type = Element.FIRE
                                grid[ny][nx].life = 10
                            }
                        }
                    }
                }

                val dir = if (seen) {
                    if (hx > x) 1 else -1
                } else {
                    if (rint(0, 1) == 1) 1 else -1
                }

                if (!walk_try(x + dir, y)) {
                    if (in_bounds(x + dir, y - 1) &&
                        empty(grid[y - 1][x + dir]) &&
                        empty(grid[y - 1][x]) &&
                        chance(70)
                    ) {
                        swapCells(grid[y - 1][x], cell)
                    } else {
                        walk_try(x + if (rint(0, 1) == 1) 1 else -1, y)
                    }
                }

                updated[y][x] = true
                continue
            }

            // --- wet dirt drying ---
            if (t == Element.WET_DIRT) {
                var nearWater = false
                for (dy in -1..1) {
                    if (nearWater) break
                    for (dx in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (!in_bounds(nx, ny)) continue
                        val ne = grid[ny][nx].type
                        if (ne == Element.WATER || ne == Element.SALTWATER) {
                            nearWater = true
                            break
                        }
                    }
                }
                if (!nearWater) {
                    cell.life--
                    if (cell.life <= 0) {
                        cell.type = Element.DIRT
                        cell.life = 0
                    }
                }
                updated[y][x] = true
                continue
            }

            // --- plants & seaweed ---
            if (t == Element.PLANT || t == Element.SEAWEED) {
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (!in_bounds(nx, ny)) continue
                        if (grid[ny][nx].type == Element.FIRE || grid[ny][nx].type == Element.LAVA) {
                            cell.type = Element.FIRE
                            cell.life = 20
                        }
                    }
                }

                if (cell.type == Element.FIRE) {
                    updated[y][x] = true
                    continue
                }

                if (t == Element.PLANT) {
                    val goodSoil = in_bounds(x, y + 1) && grid[y + 1][x].type == Element.WET_DIRT
                    if (goodSoil && chance(2)) {
                        val gx = x
                        val gy = y - 1
                        if (in_bounds(gx, gy) && empty(grid[gy][gx])) {
                            grid[gy][gx].type = Element.PLANT
                            grid[gy][gx].life = 0
                        }
                    }
                } else {
                    val underwater = in_bounds(x, y - 1) &&
                        (grid[y - 1][x].type == Element.WATER || grid[y - 1][x].type == Element.SALTWATER)
                    val isTop = !in_bounds(x, y - 1) || grid[y - 1][x].type != Element.SEAWEED
                    if (underwater && isTop && chance(2)) {
                        val gy = y - 1
                        if (in_bounds(x, gy) &&
                            (grid[gy][x].type == Element.WATER || grid[gy][x].type == Element.SALTWATER)
                        ) {
                            grid[gy][x].type = Element.SEAWEED
                            grid[gy][x].life = 0
                        }
                    }
                }
                updated[y][x] = true
                continue
            }

            // --- wood/coal burn ---
            if (t == Element.WOOD || t == Element.COAL) {
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (!in_bounds(nx, ny)) continue
                        if (grid[ny][nx].type == Element.FIRE || grid[ny][nx].type == Element.LAVA) {
                            cell.type = Element.FIRE
                            cell.life = if (t == Element.COAL) 35 else 25
                        }
                    }
                }
                updated[y][x] = true
                continue
            }

            // --- gunpowder ---
            if (t == Element.GUNPOWDER) {
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (!in_bounds(nx, ny)) continue
                        val ne = grid[ny][nx].type
                        if (ne == Element.FIRE || ne == Element.LAVA) {
                            explode(x, y, 5)
                            break
                        }
                    }
                }
                updated[y][x] = true
                continue
            }

            // --- wire / metal conduction ---
            if (t == Element.WIRE || t == Element.METAL) {
                if (cell.life > 0) {
                    val q = cell.life
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = x + dx
                            val ny = y + dy
                            if (!in_bounds(nx, ny)) continue
                            val n = grid[ny][nx]
                            if (n.type == Element.WIRE || n.type == Element.METAL) {
                                if (n.life < q - 1) n.life = q - 1
                            }
                            if (n.type == Element.WATER || n.type == Element.SALTWATER) {
                                if (n.life < q - 1) n.life = q - 1
                            }
                            if (flammable(n.type) && chance(15)) {
                                if (n.type == Element.GUNPOWDER) explode(nx, ny, 5)
                                else {
                                    n.type = Element.FIRE
                                    n.life = 15 + rint(0, 10)
                                }
                            }
                            if (n.type == Element.HYDROGEN || n.type == Element.GAS) {
                                if (chance(35)) explode(nx, ny, 4)
                            }
                        }
                    }
                    cell.life--
                    if (cell.life < 0) cell.life = 0
                }
                updated[y][x] = true
                continue
            }

            // --- ice ---
            if (t == Element.ICE) {
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (!in_bounds(nx, ny)) continue
                        val ne = grid[ny][nx].type
                        if (ne == Element.FIRE || ne == Element.LAVA || ne == Element.STEAM) {
                            if (chance(25)) {
                                cell.type = Element.WATER
                                cell.life = 0
                            }
                        }
                    }
                }
                updated[y][x] = true
                continue
            }

            updated[y][x] = true
        }
    }
}

// ===== Drawing =====
private fun draw_grid(cx: Int, cy: Int, cur: Element, paused: Boolean, brush: Int) {
    for (y in 0 until gHeight) {
        for (x in 0 until gWidth) {
            val c = grid[y][x]
            var ch = glyph_of(c.type)

            if (c.type == Element.HUMAN) ch = if ((c.life / 6) % 2 != 0) 'y' else 'Y'
            if (c.type == Element.ZOMBIE) ch = if ((c.life / 6) % 2 != 0) 't' else 'T'
            if (c.type == Element.LIGHTNING) ch = '|'

            var col = color_of(c.type)
            if ((c.type == Element.WATER || c.type == Element.SALTWATER) && c.life > 0) {
                col = 9
            }

            if (has_colors()) attron(COLOR_PAIR(col))
            mvaddch(y, x, ch)
            if (has_colors()) attroff(COLOR_PAIR(col))
        }
    }

    if (in_bounds(cx, cy)) mvaddch(cy, cx, '+')

    val (maxy, maxx) = getmaxyx(stdscr)
    if (gHeight < maxy) mvhline(gHeight, 0, '-', maxx)

    var status =
        "Move: Arrows/WASD | Space: draw | E: erase | +/-: brush | C/X: clear | " +
            "P: pause | M/Tab: elements | Q: quit"
    if (status.length > maxx) status = status.substring(0, maxx)
    if (gHeight + 1 < maxy) mvaddnstr(gHeight + 1, 0, status, maxx)

    var info = "Current: ${name_of(cur)} | Brush r=$brush" + if (paused) " [PAUSED]" else ""
    if (info.length > maxx) info = info.substring(0, maxx)
    if (gHeight + 2 < maxy) mvaddnstr(gHeight + 2, 0, info, maxx)
}

// ===== Element Browser & Credits =====
enum class Category { POWDERS, LIQUIDS, SOLIDS, GASES, SPECIAL, CREDITS }
data class MenuItem(val type: Element, val cat: Category, val label: String, val desc: String)

private val MENU = listOf(
    // Powders
    MenuItem(Element.SAND, Category.POWDERS, "Sand", "Classic falling grains."),
    MenuItem(Element.GUNPOWDER, Category.POWDERS, "Gunpowder", "Explodes when ignited."),
    MenuItem(Element.ASH, Category.POWDERS, "Ash", "Burnt residue."),
    MenuItem(Element.SNOW, Category.POWDERS, "Snow", "Melts near heat."),

    // Liquids
    MenuItem(Element.WATER, Category.LIQUIDS, "Water", "Flows, cools, extinguishes."),
    MenuItem(Element.SALTWATER, Category.LIQUIDS, "Salt Water", "Conductive water."),
    MenuItem(Element.OIL, Category.LIQUIDS, "Oil", "Light, flammable."),
    MenuItem(Element.ETHANOL, Category.LIQUIDS, "Ethanol", "Very flammable."),
    MenuItem(Element.ACID, Category.LIQUIDS, "Acid", "Dissolves many materials."),
    MenuItem(Element.LAVA, Category.LIQUIDS, "Lava", "Hot molten rock."),
    MenuItem(Element.MERCURY, Category.LIQUIDS, "Mercury", "Heavy liquid metal."),

    // Solids
    MenuItem(Element.STONE, Category.SOLIDS, "Stone", "Heavy solid block."),
    MenuItem(Element.GLASS, Category.SOLIDS, "Glass", "From sand + lava."),
    MenuItem(Element.WALL, Category.SOLIDS, "Wall", "Indestructible barrier."),
    MenuItem(Element.WOOD, Category.SOLIDS, "Wood", "Flammable solid."),
    MenuItem(Element.PLANT, Category.SOLIDS, "Plant", "Grows on wet dirt."),
    MenuItem(Element.SEAWEED, Category.SOLIDS, "Seaweed", "Grows in water over sand."),
    MenuItem(Element.METAL, Category.SOLIDS, "Metal", "Conductive solid."),
    MenuItem(Element.WIRE, Category.SOLIDS, "Wire", "Conductive path."),
    MenuItem(Element.ICE, Category.SOLIDS, "Ice", "Melts into water."),
    MenuItem(Element.COAL, Category.SOLIDS, "Coal", "Burns longer."),
    MenuItem(Element.DIRT, Category.SOLIDS, "Dirt", "Gets wet; grows plants."),
    MenuItem(Element.WET_DIRT, Category.SOLIDS, "Wet Dirt", "Dries over time."),

    // Gases
    MenuItem(Element.SMOKE, Category.GASES, "Smoke", "Rises; may fall as ash."),
    MenuItem(Element.STEAM, Category.GASES, "Steam", "Condenses to water."),
    MenuItem(Element.GAS, Category.GASES, "Gas", "Neutral rising gas."),
    MenuItem(Element.TOXIC_GAS, Category.GASES, "Toxic Gas", "Nasty chemical cloud."),
    MenuItem(Element.HYDROGEN, Category.GASES, "Hydrogen", "Very light, explosive."),
    MenuItem(Element.CHLORINE, Category.GASES, "Chlorine", "Harms plants."),

    // Special
    MenuItem(Element.FIRE, Category.SPECIAL, "Fire", "Burns & flickers upward."),
    MenuItem(Element.LIGHTNING, Category.SPECIAL, "Lightning", "Yellow electrical bolt."),
    MenuItem(Element.HUMAN, Category.SPECIAL, "Human", "Avoids zombie, fights back."),
    MenuItem(Element.ZOMBIE, Category.SPECIAL, "Zombie", "Chases and infects humans."),
    MenuItem(Element.EMPTY, Category.SPECIAL, "Eraser", "Place empty space."),

    // Credits tab
    MenuItem(Element.EMPTY, Category.CREDITS, "Credits", "Show credits & license.")
)

private fun cat_name(c: Category): String =
    when (c) {
        Category.POWDERS -> "Powders"
        Category.LIQUIDS -> "Liquids"
        Category.SOLIDS -> "Solids"
        Category.GASES -> "Gases"
        Category.SPECIAL -> "Special"
        Category.CREDITS -> "Credits"
    }

// Use a separate ncurses window for credits to avoid flicker / messing stdscr
private fun show_credits_overlay() {
    val (maxy, maxx) = getmaxyx(stdscr)
    if (maxx < 40 || maxy < 12) return

    val w = min(maxx - 4, 70)
    val h = min(maxy - 4, 15)
    val ty = (maxy - h) / 2
    val lx = (maxx - w) / 2

    val win = newwin(h, w, ty, lx)
    if (win == Pointer.NULL) return

    box(win, 0.toChar(), 0.toChar())
    val title = " Credits "
    mvwaddnstr(win, 0, (w - title.length) / 2, title, w - 2)

    val lines = arrayOf(
        "Terminal Powder Toy-like Sandbox",
        "Author: Robert",
        "GitHub: https://github.com/RobertFlexx",
        "Language: Kotlin + ncurses + JNA",
        "",
        "BSD 3-Clause License (snippet):",
        "Redistribution and use in source and binary forms,",
        "with or without modification, are permitted provided",
        "that the following conditions are met:",
        "1) Source redistributions retain this notice & disclaimer.",
        "2) Binary redistributions reproduce this notice & disclaimer.",
        "3) Names of contributors can't be used to endorse products",
        "   derived from this software without permission.",
        "",
        "Press any key to return."
    )
    var y = 2
    for (i in lines.indices) {
        if (y >= h - 1) break
        mvwaddnstr(win, y, 2, lines[i], w - 4)
        y++
    }

    wrefresh(win)
    flushinp()
    wgetch(win)
    delwin(win)
}

private fun element_menu(current: Element): Element {
    val tabs = arrayOf(
        Category.POWDERS,
        Category.LIQUIDS,
        Category.SOLIDS,
        Category.GASES,
        Category.SPECIAL,
        Category.CREDITS
    )
    val NT = tabs.size

    var curTab = Category.POWDERS
    for (it in MENU) {
        if (it.type == current) {
            curTab = it.cat
            break
        }
    }

    var tabIdx = 0
    for (i in 0 until NT) {
        if (tabs[i] == curTab) {
            tabIdx = i
            break
        }
    }

    var sel = 0
    var done = false
    var result = current

    while (!done) {
        val (maxy, maxx) = getmaxyx(stdscr)
        val idx = mutableListOf<Int>()
        for (i in MENU.indices) {
            if (MENU[i].cat == tabs[tabIdx]) idx.add(i)
        }

        if (sel < 0) sel = 0
        if (sel >= idx.size) sel = idx.size - 1

        var boxW = max(44, maxx - 6)
        var boxH = max(14, maxy - 6)
        boxW = min(boxW, maxx)
        boxH = min(boxH, maxy)
        val lx = (maxx - boxW) / 2
        val ty = (maxy - boxH) / 2
        val rx = lx + boxW - 1
        val by = ty + boxH - 1

        erase()
        mvaddch(ty, lx, '+')
        mvaddch(ty, rx, '+')
        mvaddch(by, lx, '+')
        mvaddch(by, rx, '+')
        for (x in lx + 1 until rx) {
            mvaddch(ty, x, '-')
            mvaddch(by, x, '-')
        }
        for (y in ty + 1 until by) {
            mvaddch(y, lx, '|')
            mvaddch(y, rx, '|')
        }

        val title = " Element Browser "
        mvaddnstr(ty, lx + (boxW - title.length) / 2, title, boxW - 2)

        val tabsY = ty + 1
        var cx = lx + 2
        for (i in 0 until NT) {
            var tab = " "
            tab += cat_name(tabs[i])
            tab += " "
            if (cx + tab.length >= rx) break
            if (i == tabIdx) attron(A_REVERSE)
            mvaddnstr(tabsY, cx, tab, rx - cx - 1)
            if (i == tabIdx) attroff(A_REVERSE)
            cx += tab.length + 1
        }

        var y = ty + 3
        val maxListY = by - 3
        for (i in idx.indices) {
            if (y > maxListY) break
            val it = MENU[idx[i]]
            var line = " "
            line += it.label
            line += " - "
            line += it.desc
            if (line.length > boxW - 4) line = line.substring(0, boxW - 4)
            if (i == sel) attron(A_REVERSE)
            mvaddnstr(y, lx + 2, line, boxW - 4)
            if (i == sel) attroff(A_REVERSE)
            y++
        }

        val hint = "Left/Right: tabs | Up/Down: select | Enter: choose | ESC: back"
        mvaddnstr(by - 1, lx + 2, hint, boxW - 4)
        refresh()

        val ch = getch()
        when (ch) {
            KEY_LEFT -> {
                tabIdx = (tabIdx + NT - 1) % NT
                sel = 0
            }
            KEY_RIGHT -> {
                tabIdx = (tabIdx + 1) % NT
                sel = 0
            }
            KEY_UP -> {
                if (idx.isNotEmpty()) sel = (sel + idx.size - 1) % idx.size
            }
            KEY_DOWN -> {
                if (idx.isNotEmpty()) sel = (sel + 1) % idx.size
            }
            '\n'.toInt(), '\r'.toInt(), KEY_ENTER -> {
                if (idx.isNotEmpty()) {
                    val it = MENU[idx[sel]]
                    if (it.cat == Category.CREDITS) {
                        show_credits_overlay()
                    } else {
                        result = it.type
                        done = true
                    }
                } else {
                    done = true
                }
            }
            27 -> {
                done = true
            }
        }
    }
    return result
}

// ===== Main =====
fun main() {
    initscr()
    cbreak()
    noecho()
    curs_set(0)
    keypad(stdscr, true)
    nodelay(stdscr, true)

    val (termH0, termW0) = getmaxyx(stdscr)
    val simH0 = max(1, termH0 - 3)
    init_grid(termW0, simH0)

    if (has_colors()) {
        start_color()
        use_default_colors()
        init_pair(1, COLOR_BLACK, -1)
        init_pair(2, COLOR_YELLOW, -1)   // sand/dirt/etc
        init_pair(3, COLOR_CYAN, -1)     // water-ish
        init_pair(4, COLOR_WHITE, -1)    // neutral solids
        init_pair(5, COLOR_GREEN, -1)    // plants/humans
        init_pair(6, COLOR_RED, -1)      // fire/lava/zombies
        init_pair(7, COLOR_MAGENTA, -1)  // smoke/gas/ash
        init_pair(8, COLOR_BLUE, -1)     // oil/mercury
        init_pair(9, COLOR_YELLOW, -1)   // lightning/acid/etc
    }

    var cx = gWidth / 2
    var cy = gHeight / 2
    var brush = 1
    var current = Element.SAND
    var running = true
    var paused = false

    while (running) {
        val (nh, nw) = getmaxyx(stdscr)
        val nSimH = max(1, nh - 3)
        if (nw != gWidth || nSimH != gHeight) {
            init_grid(nw, nSimH)
            cx = cx.coerceIn(0, gWidth - 1)
            cy = cy.coerceIn(0, gHeight - 1)
        }

        var ch: Int
        while (true) {
            ch = getch()
            if (ch == ERR) break
            when (ch) {
                'q'.toInt(), 'Q'.toInt() -> running = false
                KEY_LEFT, 'a'.toInt(), 'A'.toInt() -> cx = max(0, cx - 1)
                KEY_RIGHT, 'd'.toInt(), 'D'.toInt() -> cx = min(gWidth - 1, cx + 1)
                KEY_UP, 'w'.toInt() -> cy = max(0, cy - 1)
                KEY_DOWN, 's'.toInt(), 'S'.toInt() -> cy = min(gHeight - 1, cy + 1)
                ' '.toInt() -> place_brush(cx, cy, brush, current)
                'e'.toInt(), 'E'.toInt() -> place_brush(cx, cy, brush, Element.EMPTY)
                '+'.toInt(), '='.toInt() -> if (brush < 8) brush++
                '-'.toInt(), '_'.toInt() -> if (brush > 1) brush--
                'c'.toInt(), 'C'.toInt(), 'x'.toInt(), 'X'.toInt() -> clear_grid()
                'p'.toInt(), 'P'.toInt() -> paused = !paused
                'm'.toInt(), 'M'.toInt(), '\t'.toInt() -> {
                    flushinp()
                    nodelay(stdscr, false)
                    current = element_menu(current)
                    nodelay(stdscr, true)
                }
                '1'.toInt() -> current = Element.SAND
                '2'.toInt() -> current = Element.WATER
                '3'.toInt() -> current = Element.STONE
                '4'.toInt() -> current = Element.WOOD
                '5'.toInt() -> current = Element.FIRE
                '6'.toInt() -> current = Element.OIL
                '7'.toInt() -> current = Element.LAVA
                '8'.toInt() -> current = Element.PLANT
                '9'.toInt() -> current = Element.GUNPOWDER
                '0'.toInt() -> current = Element.ACID
                'W'.toInt() -> current = Element.WALL
                'L'.toInt() -> current = Element.LIGHTNING
                'H'.toInt(), 'h'.toInt() -> current = Element.HUMAN
                'Z'.toInt() -> current = Element.ZOMBIE
                'D'.toInt() -> current = Element.DIRT
            }
        }

        if (!paused) step_sim()

        erase()
        draw_grid(cx, cy, current, paused, brush)
        refresh()
        napms(16)
    }

    endwin()
}
