
var exec = require("cordova/exec");

var Pedometer = function () {
    this.name = "Pedometer";
};

Pedometer.prototype.isStepCountingAvailable = function (onSuccess, onError) {
    exec(onSuccess, onError, "Pedometer", "isStepCountingAvailable", []);
};

Pedometer.prototype.isDistanceAvailable = function (onSuccess, onError) {
    exec(onSuccess, onError, "Pedometer", "isDistanceAvailable", []);
};

Pedometer.prototype.isFloorCountingAvailable = function (onSuccess, onError) {
    exec(onSuccess, onError, "Pedometer", "isFloorCountingAvailable", []);
};


module.exports = new Pedometer();
