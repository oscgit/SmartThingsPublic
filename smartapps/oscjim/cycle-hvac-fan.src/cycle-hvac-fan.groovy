/**
 *  Cycle HVAC Fan
 * 
 *  This SmartApp allows cycling of an HVAC fan on a schedule.  If a change is made to the heating
 *  or cooling temperator setting of the HVAC device, circulation is suspended.
 *
 *  Copyright 2016 Jim Young
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
definition(
    name: "Cycle HVAC Fan",
    namespace: "oscjim",
    author: "jyoung@olympicsolutions.com",
    description: "Run fan every X minutes if AC or HEAT is not on.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

preferences {
	section("Title") {
		paragraph "Cycle HVAC Fan"
	}
	section("About") {
        paragraph "Run HVAC fan every X minutes if AC or heat has not been on."
    }
    section("Thermostat") {
        input "thermostat", "capability.thermostat", title:"Select thermostat to be controlled"
        input "duration", "number", title:"Set the duration of each circulation cycle (in minutes)", defaultValue:5
        input "interval", "number", title:"Set time between circulation cycles (in minutes)", defaultValue:30
	}
}

def installed() {
	log.debug("Installed with settings: ${settings}")
    initialize()
}

def updated() {
	log.debug("Updated with settings: ${settings}")
	unsubscribe()
	unschedule()
   	initialize()
}

def initialize() {
	log.debug("initialize()")
    
    // Listen for changes to heating or cooling temperature settings.
    subscribe(thermostat, "heatingSetpoint", eventHandler)
    subscribe(thermostat, "coolingSetpoint", eventHandler)

    // Register the circulation schedule.
    def interval = settings.interval.toInteger();
    log.debug("initialize() - setting circulation cron schedule -> 11 0/${interval} * * * ?");
    schedule("11 0/${interval} * * * ?", startCirculation);
}

/**
 * Starts a circulation cycle and schedules termination of the cycle if the
 * thermostate is at idle.
 */
def startCirculation() {
	def currentState = thermostat.currentValue("thermostatOperatingState");
	log.debug("startCirculation() - theromstateOperatingState is ${currentState}");
	if (currentState == "idle") {
    	log.debug("startCirculation() - starting circulation cycle.");
    	thermostat.fanOn();
        def duration = settings.duration.toInteger();
        log.debug("startCirculation() - scheduling stopCirculation in ${duration} minutes.");
        runIn(duration*60, stopCirculation);
    } else {
    	log.debug("startCirculation() - skipping this cycle because not in idle state.");
    }
}

def stopCirculation() {
	log.debug("stopCirculation()");
	thermostat.fanAuto();
}

/**
 * Terminate circulation if the termostate is asked for heating or cooling.
 */
def eventHandler(evt) {
	def currentState = thermostat.currentValue("thermostatOperatingState");
	log.debug("eventHandler(): ${evt.value}: ${evt}, ${settings}");
	log.debug("eventHandler() - theromstateOperatingState is ${currentState}");
    
    if (currentState == "heating" || currentState == "cooling") {
		log.debug("heating or cooling mode is active - aborting circulation mode.")
    	stopCirculation();
    } 
}
