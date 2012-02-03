goog.provide('vtst');
goog.require('foo');
goog.require('goog.dom');
goog.require('goog.dom.forms');

/**
 * This is the description.
 * @param {number} x
 * @param {number} y
 * @constructor
 */
vtst.foo = function(x, y) {
	return (x + y);
}

/** @constructor
 */
var test = function(){
	return test_vtst;
};

/**
 * This is the second description.
 * @param {Date} zoopy
 * @param {Foo} y
 * @constructor
 * @return {Date}
 */
vtst.bar = function(zoopy, y) {
	var myvar = 1;
	var x = (12 + 4 + 3);
    vtst.bar(zoopy, y);
}