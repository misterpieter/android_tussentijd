    val locations = "geoLocations.json"
    val json = application.assets.open(locations)
    val parser: Parser = Parser.default()
    val array = parser.parse(json) as JsonArray<JsonObject>