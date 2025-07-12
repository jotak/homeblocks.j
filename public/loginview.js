"use strict";
angular.module('homeblocks.loginview', ['ngRoute'])
    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/login', {
            templateUrl: 'mainview.html',
            controller: 'loginViewCtrl'
        });
    }]).controller("loginViewCtrl", ['$scope', '$http', '$routeParams', '$rootScope', '$location', function ($scope, $http, $routeParams, $rootScope, $location) {
        $rootScope.title = "homeblocks";
        $http.get('/api/login').success(function(ctx) {
            $rootScope.title = ctx.title;
            $scope.page = ctx.page;
            $scope.minPos = {x: 0, y: 0};
            fillPageStyle($scope.page.blocks, $scope.minPos, true);
        }).error(function (data) {
            console.log('Error: ' + data);
        });
    }]);
