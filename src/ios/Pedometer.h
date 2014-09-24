//
//  Pedometer.h
//  Copyright (c) 2014 Lee Crossley - http://ilee.co.uk
//

#import "Foundation/Foundation.h"
#import "Cordova/CDV.h"

@interface Pedometer : CDVPlugin

- (void) isStepCountingAvailable:(CDVInvokedUrlCommand*)command;
- (void) isDistanceAvailable:(CDVInvokedUrlCommand*)command;
- (void) isFloorCountingAvailable:(CDVInvokedUrlCommand*)command;

- (void) startPedometerUpdatesFromDate:(CDVInvokedUrlCommand*)command;
- (void) stopPedometerUpdates:(CDVInvokedUrlCommand*)command;

@end
