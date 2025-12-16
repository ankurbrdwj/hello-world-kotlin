  ---
Partner Service - Deep Dive

1. Core Components

┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              COMPONENT ARCHITECTURE                                      │
└─────────────────────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────────────────┐
│                                                                                        │
│   ┌─────────────────────┐      ┌─────────────────────┐      ┌─────────────────────┐  │
│   │      Generator      │      │ QuotesWebSocketServer│     │InstrumentWebSocket  │  │
│   │   (Price Engine)    │─────▶│  (Broadcast Quotes)  │     │Server (Broadcast    │  │
│   │                     │      │                      │     │   Instruments)      │  │
│   └──────────┬──────────┘      └──────────────────────┘     └─────────────────────┘  │
│              │                           ▲                            ▲               │
│              │                           │                            │               │
│   ┌──────────▼──────────┐               │                            │               │
│   │RandomWalkInstrument │      ┌────────┴────────────────────────────┘               │
│   │ (Price Algorithm)   │      │                                                      │
│   └─────────────────────┘      │  WebSocket Connections                              │
│                                │  (Multiple clients can connect)                      │
│                                │                                                      │
└────────────────────────────────┼──────────────────────────────────────────────────────┘
│
▼
┌────────────────────────┐
│     Web Clients        │
│  (Browser / Your App)  │
└────────────────────────┘

  ---
2. Data Models Explained

// INSTRUMENT - Represents a stock/security
┌─────────────────────────────────────────────────────────────────┐
│  class Instrument                                               │
│  ───────────────                                                │
│                                                                 │
│  isin: String        →  "DE0007164600"  (International ID)     │
│  description: String →  "SAP SE"        (Company name)         │
│                                                                 │
│  Example JSON output:                                           │
│  {                                                              │
│    "isin": "DE0007164600",                                     │
│    "description": "SAP SE"                                     │
│  }                                                              │
└─────────────────────────────────────────────────────────────────┘

// QUOTE - Represents a price tick
┌─────────────────────────────────────────────────────────────────┐
│  class Quote                                                    │
│  ───────────                                                    │
│                                                                 │
│  isin: String         →  "DE0007164600"                        │
│  price: BigDecimal    →  125.4567       (Precise price)        │
│                                                                 │
│  Example JSON output:                                           │
│  {                                                              │
│    "isin": "DE0007164600",                                     │
│    "price": 125.4567                                           │
│  }                                                              │
└─────────────────────────────────────────────────────────────────┘

// WEBSOCKET MESSAGE - Wrapper for all messages
┌─────────────────────────────────────────────────────────────────┐
│  class WebsocketMessage                                         │
│  ──────────────────────                                         │
│                                                                 │
│  type: MessageType    →  ADD / DELETE / QUOTE                  │
│  data: JsonPrintable  →  Instrument or Quote object            │
│                                                                 │
│  Example JSON outputs:                                          │
│                                                                 │
│  // When new instrument added:                                  │
│  {                                                              │
│    "type": "ADD",                                              │
│    "data": {"isin": "DE0007164600", "description": "SAP SE"}   │
│  }                                                              │
│                                                                 │
│  // When instrument removed:                                    │
│  {                                                              │
│    "type": "DELETE",                                           │
│    "data": {"isin": "DE0007164600", "description": "SAP SE"}   │
│  }                                                              │
│                                                                 │
│  // When price update:                                          │
│  {                                                              │
│    "type": "QUOTE",                                            │
│    "data": {"isin": "DE0007164600", "price": 125.4567}         │
│  }                                                              │
└─────────────────────────────────────────────────────────────────┘

  ---
3. RandomWalkInstrument - The Price Algorithm

This is the heart of price generation. It uses a "Random Walk" algorithm - the same concept used in financial modeling!

┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                          RANDOM WALK PRICE GENERATION                                    │
└─────────────────────────────────────────────────────────────────────────────────────────┘

    class RandomWalkInstrument:
    ───────────────────────────

    Fields:
    ┌────────────────────┬────────────────────────────────────────────────────────────────┐
    │ instrument         │ The stock (Instrument object)                                  │
    │ startPrice         │ Starting price for this walk cycle                             │
    │ targetPrice        │ Target price to walk towards                                   │
    │ targetSteps        │ How many steps to reach target                                 │
    │ step               │ Current step number                                            │
    │ lastPrice          │ Last generated price                                           │
    │ stepWidth          │ Price change per step = (target - start) / steps              │
    └────────────────────┴────────────────────────────────────────────────────────────────┘

    How nextPrice() works:
    ──────────────────────

    Step 0:  startPrice = 100,  targetPrice = 110,  targetSteps = 10
             stepWidth = (110 - 100) / 10 = 1.0

    Step 1:  lastPrice = 100 + 1.0 + random_noise = 101.23
    Step 2:  lastPrice = 101.23 + 1.0 + random_noise = 102.45
    Step 3:  lastPrice = 102.45 + 1.0 + random_noise = 103.12
    ...
    Step 10: lastPrice ≈ 110 (reached target)
             → reset() called → pick NEW random target


    Visual:
    ────────

    Price
      ▲
      │                              ╭─────── New target
      │                        ╭────╯
      │                   ╭────╯
      │              ╭────╯
      │         ╭────╯
      │    ╭────╯
      │────╯ Start
      │
      └──────────────────────────────────────────▶ Time

      Not a straight line! Has random "noise" at each step.
      This mimics real stock price movements!

  ---
4. Generator - The Orchestrator

┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              GENERATOR - Main Loop                                       │
└─────────────────────────────────────────────────────────────────────────────────────────┘

    class Generator:
    ─────────────────

    Fields:
    - instruments: List<RandomWalkInstrument>   → All active instruments
    - instrumentServer: InstrumentWebSocketServer
    - quotesServer: QuotesWebSocketServer


    What Generator does (pseudo-code):
    ───────────────────────────────────

    // On startup - runs in background thread
    while (true) {

        // 1. Maybe add a new instrument (randomly)
        if (random() < 0.1 && instruments.size < maxInstruments) {
            addInstrument()
            // → Creates new Instrument with random ISIN
            // → Creates RandomWalkInstrument for price generation
            // → Broadcasts ADD message to /instruments endpoint
        }

        // 2. Maybe remove an instrument (randomly)
        if (random() < 0.05 && instruments.size > minInstruments) {
            removeInstrument()
            // → Picks random instrument
            // → Broadcasts DELETE message to /instruments endpoint
            // → Removes from list
        }

        // 3. Generate price for random instrument
        instrument = pickRandom(instruments)
        newPrice = instrument.nextPrice()
        quote = Quote(instrument.isin, newPrice)

        // 4. Broadcast to all connected clients
        quotesServer.sendPrice(quote)
        // → Sends to /quotes WebSocket endpoint

        // 5. Wait before next tick
        sleep(avgMillisecondsBetweenQuotes)  // e.g., 100-500ms
    }

  ---
5. WebSocket Servers - Broadcasting

┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                         QuotesWebSocketServer                                            │
└─────────────────────────────────────────────────────────────────────────────────────────┘

    class QuotesWebSocketServer:
    ────────────────────────────

    sockets: CopyOnWriteArrayList<WebSocketSession>
    // Thread-safe list of all connected clients


    ┌──────────────────┐
    │ Client 1 Browser │──┐
    └──────────────────┘  │
                          │     ┌─────────────────────────┐
    ┌──────────────────┐  ├────▶│   sockets list          │
    │ Client 2 (App)   │──┤     │   [session1, session2,  │
    └──────────────────┘  │     │    session3]            │
                          │     └─────────────────────────┘
    ┌──────────────────┐  │                │
    │ Client 3 (App)   │──┘                │
    └──────────────────┘                   ▼


    sendPrice(quote):
    ─────────────────

    message = WebsocketMessage(type=QUOTE, data=quote)
    json = message.asJson().toString()

    for each socket in sockets:
        socket.send(json)   // Broadcast to ALL clients


    // Every connected client receives:
    {"type":"QUOTE","data":{"isin":"DE0007164600","price":125.45}}

  ---
6. Complete Data Flow Timeline

┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              TIMELINE - What Gets Spit Out                               │
└─────────────────────────────────────────────────────────────────────────────────────────┘

Time    Event                          /instruments endpoint        /quotes endpoint
────    ─────                          ────────────────────        ─────────────────

0ms     Server starts                  -                           -
Generator creates 5
instruments

10ms    Instrument 1 added            {"type":"ADD",               -
"data":{"isin":"AB123",
"description":"Stock A"}}

20ms    Instrument 2 added            {"type":"ADD",               -
"data":{"isin":"CD456",...}}

50ms    Price tick                    -                            {"type":"QUOTE",
"data":{"isin":"AB123",
"price":100.50}}

150ms   Price tick                    -                            {"type":"QUOTE",
"data":{"isin":"CD456",
"price":250.75}}

250ms   Price tick                    -                            {"type":"QUOTE",
"data":{"isin":"AB123",
"price":101.23}}

300ms   Instrument removed            {"type":"DELETE",            -
"data":{"isin":"CD456",...}}

350ms   New instrument added          {"type":"ADD",               -
"data":{"isin":"EF789",...}}

400ms   Price tick                    -                            {"type":"QUOTE",
"data":{"isin":"AB123",
"price":101.89}}

...continues forever...

  ---
7. Key Learnings for Your App

┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                         WHAT WE CAN LEARN & APPLY                                        │
└─────────────────────────────────────────────────────────────────────────────────────────┘

1. SEPARATE ENDPOINTS for different data types
   ────────────────────────────────────────────
   /instruments → Metadata changes (add/delete)
   /quotes      → Price updates only

   Your app could have:
   /market-feed → Price updates
   /alerts      → Alert status changes


2. MESSAGE WRAPPER with TYPE field
   ─────────────────────────────────
   {"type": "QUOTE", "data": {...}}

   This allows client to handle different message types:
    - QUOTE → update price display
    - ADD   → add to watchlist
    - DELETE → remove from watchlist


3. RANDOM WALK for realistic prices
   ──────────────────────────────────
   Prices don't just random jump - they WALK gradually
   toward a target, mimicking real market behavior.


4. THREAD-SAFE collections
   ─────────────────────────
   CopyOnWriteArrayList for WebSocket sessions
    - Multiple clients connecting/disconnecting
    - Concurrent price broadcasts


5. BROADCAST pattern
   ──────────────────
   One price → send to ALL connected clients
   Not request-response, but PUSH model.

  ---
8. Sample Output (What You'll See)

When you run the JAR and connect, you'll receive messages like this:

// On /instruments endpoint:
{"type":"ADD","data":{"isin":"KQ7E15L3R6D8","description":"KQ7E15L3R6D8"}}
{"type":"ADD","data":{"isin":"MN2X89P4T1W5","description":"MN2X89P4T1W5"}}
{"type":"DELETE","data":{"isin":"KQ7E15L3R6D8","description":"KQ7E15L3R6D8"}}

// On /quotes endpoint (very frequent):
{"type":"QUOTE","data":{"isin":"MN2X89P4T1W5","price":156.7823}}
{"type":"QUOTE","data":{"isin":"MN2X89P4T1W5","price":157.0145}}
{"type":"QUOTE","data":{"isin":"MN2X89P4T1W5","price":156.8901}}
{"type":"QUOTE","data":{"isin":"MN2X89P4T1W5","price":157.2234}}

