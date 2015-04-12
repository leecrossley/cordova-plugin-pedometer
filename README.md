## Core Motion Pedometer Plugin for Apache Cordova

**Fetch pedestrian-related pedometer data, such as step counts and other information about the distance travelled.**

## Install

```
cordova plugin add https://github.com/leecrossley/cordova-plugin-pedometer.git
```

You **do not** need to reference any JavaScript, the Cordova plugin architecture will add a pedometer object to your root automatically when you build.

## Check feature support

### isStepCountingAvailable

```js
pedometer.isStepCountingAvailable(successCallback, failureCallback);
```
- => `successCallback` is called with true if the feature is supported, otherwise false
- => `failureCallback` is called if there was an error determining if the feature is supported

### isDistanceAvailable

```js
pedometer.isDistanceAvailable(successCallback, failureCallback);
```

Distance estimation indicates the ability to use step information to supply the approximate distance travelled by the user.

This capability is not supported on all devices, even with iOS 8.

### isFloorCountingAvailable

```js
pedometer.isFloorCountingAvailable(successCallback, failureCallback);
```

Floor counting indicates the ability to count the number of floors the user walks up or down using stairs.

This capability is not supported on all devices, even with iOS 8.


## Live pedometer data

### startPedometerUpdates

Starts the delivery of recent pedestrian-related data to your Cordova app.

```js
var successHandler = function (pedometerData) {
    // pedometerData.startDate; -> ms since 1970
    // pedometerData.endDate; -> ms since 1970
    // pedometerData.numberOfSteps;
    // pedometerData.distance;
    // pedometerData.floorsAscended;
    // pedometerData.floorsDescended;
};
pedometer.startPedometerUpdates(successHandler, onError);
```

The success handler is executed when data is available and is called repeatedly from a background thread as new data arrives.

When the app is suspended, the delivery of updates stops temporarily. Upon returning to foreground or background execution, the pedometer object begins updates again.

### stopPedometerUpdates

Stops the delivery of recent pedestrian data updates to your Cordova app.

```js
pedometer.stopPedometerUpdates(successCallback, failureCallback);
```

## Platform and device support

iOS 8+ only. These capabilities are not supported on all devices, even with iOS 8, so please ensure you use the *check feature support* functions.

## License

[MIT License](http://ilee.mit-license.org)
