{
	"namespace": "com.example.carmodel",
	"formatVersion": "2",
	"structuredTypes": [{
		"id": "1",
		"name": "Car",
		"label": "Car",
		"description": "A popular motor vehicle",
		"isCase": true,
		"stateModel": {
			"states": [{
				"id": "6",
				"label": "Driving",
				"value": "DRIVING"
			}, {
				"id": "7",
				"label": "Parked",
				"value": "PARKED"
			}, {
				"id": "9",
				"label": "Stolen",
				"value": "STOLEN",
				"isTerminal": true
			}, {
				"id": "10",
				"label": "Scrapped",
				"value": "SCRAPPED",
				"isTerminal": true
			}]
		},
		"attributes": [{
			"id": "2",
			"name": "registration",
			"schemaName": "registrationSN",
			"label": "Registration",
			"type": "base:Text",
			"initialValue": "A123 XYZ",
			"isIdentifier": true,
			"isSearchable": true,
			"isSummary": true,
			"constraints": [{
				"name": "length",
				"value": "12"
			}]
		}, {
			"id": "3",
			"name": "make",
			"schemaName": "makeSN",
			"label": "Make",
			"type": "base:Text"
		}, {
			"id": "4",
			"name": "model",
			"schemaName": "modelSN",
			"label": "Model",
			"isSearchable": true,
			"isSummary": true,
			"type": "base:Text"
		}, {
			"id": "5",
			"name": "state",
			"schemaName": "stateSN",
			"label": "State",
			"type": "base:Text",
			"isState": true
		}, {
			"id": "8",
			"name": "numberOfDoors",
			"schemaName": "numberOfDoorsSN",
			"label": "Number of Doors",
			"type": "base:Number",
			"initialValue": 5,
			"isSummary": true
		}]
	}, {
		"id": "2",
		"name": "Garage",
		"label": "Garage",
		"description": "A building for storing cars and junk",
		"isCase": true,
		"stateModel": {
			"states": [{
				"id": "66",
				"label": "Open",
				"value": "OPEN"
			}, {
				"id": "77",
				"label": "Closed",
				"value": "CLOSED"
			}, {
				"id": "88",
				"label": "Demolished",
				"value": "DEMOLISHED",
				"isTerminal": true
			}]
		},
		"attributes": [{
			"id": "222",
			"name": "name",
			"label": "Name",
			"type": "base:Text",
			"isIdentifier": true,
			"isSearchable": true,
			"isSummary": true,
			"constraints": [{
				"name": "length",
				"value": "12"
			}]
		}, {
			"id": "5",
			"name": "state",
			"schemaName": "stateSN",
			"label": "State",
			"type": "base:Text",
			"isState": true
		}]
	}],
	"annotations": [{
		"source": "testDM",
		"contents": [{
			"name": "Name on DM",
			"value": "Value on DM"
		}]
	}],
	"links": [{
		"id": "90001",
		"end1": {
			"name": "garage",
			"label": "Garage",
			"type": "Car"
		},
		"end2": {
			"name": "cars",
			"label": "Cars",
			"type": "Garage",
			"isArray": true
		}
	}]
}