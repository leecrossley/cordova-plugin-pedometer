## Core Motion Pedometer Plugin for Apache Cordova

**This plugin is currently under active development**

Fetch pedestrian-related pedometer data, such as step counts and other information about the distance travelled.

## Install

### Locally

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

### isFloorCountingAvailable

```js
pedometer.isFloorCountingAvailable(successCallback, failureCallback);
```

## Platform Support

iOS 8+ only.

## License

[MIT License](http://ilee.mit-license.org)
