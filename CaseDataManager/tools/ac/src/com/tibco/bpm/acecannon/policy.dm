{
  "namespace" : "org.policy-corporation",
  "formatVersion" : 2,
  "structuredTypes" : [ {
    "name" : "Policy",
    "label" : "Policy",
    "isCase" : true,
    "identifierInitialisationInfo": {
      "prefix": "ORD-",
      "suffix": "X"
    },
    "stateModel" : {
      "states" : [ {
        "label" : "Created",
        "value" : "CREATED"
      }, {
        "label" : "Cancelled",
        "value" : "CANCELLED",
        "isTerminal" : true
      } ]
    },
    "attributes" : [ {
      "name" : "number",
      "label" : "Number",
      "type" : "base:Number",
      "isIdentifier": true,
      "isSummary": true,
      "isSearchable": true
    }, {
      "name" : "state",
      "label" : "State",
      "type" : "base:Text",
      "isState" : true
    }, {
      "name" : "policyStartDate",
      "label" : "Policy Start Date",
      "description": "Date on which the policy starts",
      "type" : "base:Date",
      "isSearchable" : true,
      "isSummary" : true,
      "defaultValue" : "2016-12-25"
    }, {
      "name" : "policyStartTime",
      "label" : "Policy Start Time",
      "type" : "base:Time",
      "isSearchable" : true,
      "isSummary" : true,
      "defaultValue" : "00:01"
    }, {
      "name" : "premium",
      "label" : "Premium",
      "type" : "base:Number",
      "isSearchable" : true,
      "isSummary" : true,
      "constraints" : [ {
        "name" : "decimalPlaces",
        "value" : "2"
      },{
        "name" : "length",
        "value" : "22"
      } ]
    }, {
      "name" : "comments",
      "label" : "Comments",
      "type" : "base:Text",
      "isSearchable" : true,
      "isSummary" : true
    }, {
      "name" : "claims",
      "label" : "Claims",
      "type" : "Claim",
      "isArray" : true
    } ]
  }, {
    "name" : "Person",
    "label" : "Person",
    "description" : "A human",
    "isCase" : true,
    "identifierInitialisationInfo": {
      "prefix": "P:",
      "start": 10000,
      "minNumLength": 8
    },
    "stateModel" : {
      "states" : [ {
        "label" : "Alive",
        "value" : "ALIVE"
      }, {
        "label" : "Dead",
        "value" : "DEAD",
        "isTerminal" : true
      } ]
    },
    "attributes" : [ {
      "name" : "pcode",
      "label" : "P Code",
      "type" : "base:Text",
      "isIdentifier": true,
      "isSummary": true,
      "isSearchable": true
    },{
      "name" : "name",
      "label" : "Name",
      "type" : "base:Text",
      "constraints" : [{
      	"name": "maxValue", "value": "120"
      }]
    }, {
      "name" : "aliases",
      "label" : "Aliases",
      "type" : "base:Text",
      "isArray" : true
    }, {
      "name" : "age",
      "label" : "Age",
      "type" : "base:Number",
      "constraints" : [{
      	"name": "maxValue", "value": "120"
      }]
    }, {
      "name" : "lotteryNumbers",
      "label" : "Lottery Numbers",
      "type" : "base:Number",
      "isArray" : true,
      "constraints" : [{
      	"name": "maxValue", "value": "49"},{"name": "minValue", "value": "1"}, {"name": "minValueInclusive", "value": "true"}, {"name": "maxValueInclusive", "value": "true"
      }]
    }, {
      "name" : "dateOfBirth",
      "label" : "Date of Birth",
      "type" : "base:Date"
    }, {
      "name" : "timeOfBirth",
      "label" : "Time of Birth",
      "type" : "base:Time"
    }, {
      "name" : "homeAddress",
      "label" : "Home Address",
      "type" : "Address"
    }, {
      "name" : "workAddress",
      "label" : "Work Address",
      "type" : "Address"
    }, {
      "name" : "otherAddresses",
      "label" : "Other Addresses",
      "type" : "Address",
      "isArray" : true
    }, {
      "name" : "personState",
      "label" : "Person State",
      "type" : "base:Text",
      "isState" : true
    } ]
  }, {
    "name" : "Address",
    "label" : "Address",
    "attributes" : [ {
      "name" : "firstLine",
      "label" : "First Line",
      "type" : "base:Text"
    }, {
      "name" : "secondLine",
      "label" : "Second Line",
      "type" : "base:Text"
    } ]
  }, {
    "name" : "Claim",
    "label" : "Claim",
    "attributes" : [ {
      "name" : "date",
      "label" : "Date",
      "type" : "base:Date"
    }, {
      "name" : "blame",
      "label" : "Blame",
      "type" : "base:Text",
      "allowedValues" : [ {
        "label" : "Policyholder",
        "value" : "POLICYHOLDER"
      }, {
        "label" : "Third Party",
        "value" : "THIRD_PARTY"
      }, {
        "label" : "Act of God",
        "value" : "ACT_OF_GOD"
      }, {
        "label" : "Unknown",
        "value" : "UNKNOWN"
      } ]
    }, {
      "name" : "description",
      "label" : "Description",
      "type" : "base:Text",
      "isArray" : true
    } ]
  } ],
  "links" : [ {
    "id" : "777001",
    "end1" : {
      "type" : "Person",
      "name" : "policies",
      "label" : "Policies",
      "isArray" : true
    },
    "end2" : {
      "type" : "Policy",
      "name" : "holder",
      "label" : "Holder"
    }
  }, {
    "id" : "777002",
    "end1" : {
      "type" : "Person",
      "name" : "allowedToDrive",
      "label" : "Allowed To Drive"
    },
    "end2" : {
      "type" : "Policy",
      "name" : "namedDrivers",
      "label" : "Named Drivers",
      "isArray" : true
    }
  } ]
}