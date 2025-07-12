"use strict";
angular.module('homeblocks.mainview', ['ngRoute'])
    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/u/:user/:profile', {
            templateUrl: 'mainview.html',
            controller: 'mainViewCtrl'
        }).when('/u/:user', {
            templateUrl: 'mainview.html',
            controller: 'mainViewCtrl'
        }).when('/u', {
            templateUrl: 'mainview.html',
            controller: 'mainViewCtrl'
        });
    }]).controller("mainViewCtrl", ['$scope', '$http', '$routeParams', '$rootScope', '$location', function ($scope, $http, $routeParams, $rootScope, $location) {
        var apiPath;
        $rootScope.title = "homeblocks";
        if ($routeParams.profile) {
            apiPath = '/api/user/' + $routeParams.user + "/profile/" + $routeParams.profile;
            $scope.isOnProfile = true;
        } else if ($routeParams.user) {
            apiPath = '/api/user/' + $routeParams.user;
            $scope.isOnProfile = false;
        } else {
            // Unknown user, check for logged user
            $http.get('/api/logged').success(function(state) {
                if (state) {
                    if (state.refUser && state.profile) {
                        $location.path("/u/" + state.refUser + "/" + state.profile);
                    } else if (state.refUser) {
                        $location.path("/u/" + state.refUser);
                    } else {
                        $location.path("/u/" + state.logged);
                    }
                } else {
                    $location.path("/login");
                }
            }).error(function(data) {
                console.log('Error: ' + data);
            });
            return;
        }
        $http.get(apiPath).success(function(ctx) {
            $rootScope.title = ctx.title;
            $scope.refUser = ctx.refUser;
            $scope.profile = ctx.profile;
            $scope.strJson = JSON.stringify(ctx.page);
            $scope.page = ctx.page;
            $scope.page.message = "";
            $scope.logged = ctx.logged;
            if ($scope.logged) {
                $scope.isLoggedIn = true;
                $scope.isAtHome = $scope.logged == $routeParams.user;
            } else {
                $scope.isLoggedIn = false;
                $scope.isAtHome = false;
            }
            $scope.minPos = { x: 0, y: 0 };
            fillPageStyle($scope.page.blocks, $scope.minPos, true);
            initMainListeners($scope, $location, $http);
            $scope.page.blocks.forEach(function(b) { enterBlock($scope, b); });
        }).error(function(data) {
            console.log('Error: ' + data);
        });
    }]).directive("pressEnter", function () {
        return function (scope, element, attrs) {
            element.bind("keydown keypress", function (event) {
                if (event.which === 13) {
                    scope.$apply(function () {
                        scope.$eval(attrs.myEnter);
                    });
                    event.preventDefault();
                }
            });
        };
    }).directive('trustedUrl', function ($sce) {
        return {
            restrict: 'A',
            scope: {
                src: '='
            },
            replace: true,
            template: function (element, attrs, scope) {
                return '<' + attrs.type + ' ng-src="{{ url }}" controls></' + attrs.type + '>';
            },
            link: function (scope) {
                scope.$watch('src', function (newVal, oldVal) {
                    if (newVal !== undefined) {
                        scope.url = $sce.trustAsResourceUrl(newVal);
                    }
                });
            }
        };
    });

function initMainListeners($scope, $location, $http) {
    $scope.onNew = function(name) {
        $http.put('/api/user/' + $scope.refUser + '/profile/' + name).success(function() {
            $location.path("/u/" + $scope.refUser + "/" + name + "/e");
        }).error(function(err) {
            console.error(err);
            $scope.page.message = err;
        });
    };
    $scope.onUpload = function(uploaded) {
        if (confirm("Uploading json will erase the current profile. Continue?")) {
            $scope.showUpload = !$scope.showUpload;
            $scope.page = eval('(' + uploaded + ')');
            saveProfile($http, $scope);
            $scope.minPos = {x: 0, y: 0};
            fillPageStyle($scope.page.blocks, $scope.minPos, true);
        }
    };
    $scope.onClickNew = function() {
        $scope.showNew = !$scope.showNew;
        setTimeout(function () {
            document.getElementById('newName').focus();
        }, 30);
    };
    $scope.onClickEdit = function() {
        $location.path("/u/" + $scope.refUser + "/" + $scope.profile + "/e");
    };
    $scope.onClickUpload = function() {
        $scope.showUpload = !$scope.showUpload;
        setTimeout(function () {
            document.getElementById('ulJson').focus();
        }, 30);
    };
    $scope.onClickDownload = function() {
        $scope.showJson = !$scope.showJson;
        setTimeout(function () {
            document.getElementById('dlJson').focus();
        }, 30);
    };
    $scope.onClickLogin = function() {
        $http.post('/api/login', {refUser: $scope.refUser, profile: $scope.profile}).success(function(ctx) {
            ctx.page.blocks.forEach(function(b) { b.animate = true; });
            mergeInPage($scope.page, ctx.page.blocks);
            $scope.minPos = {x: 0, y: 0};
            fillPageStyle($scope.page.blocks, $scope.minPos);
        }).error(function (data) {
            console.log('Error: ' + data);
        });
    };
    $scope.onClickLogout = function() {
        $http.get('/api/logout').success(function() {
            $scope.isLoggedIn = false;
            $scope.isAtHome = false;
        }).error(function(data) {
            console.log('Error: ' + data);
        });
    };
    $scope.onClickHome = function() {
        $location.path("/u/" + $scope.logged);
    };
    $scope.onClickAlias = function() {
        $scope.showAlias = !$scope.showAlias;
        $scope.alias = $scope.logged;
        setTimeout(function () {
            document.getElementById('alias').focus();
        }, 30);
    };
    $scope.onSetAlias = function(name) {
        $http.put('/api/alias/' + name).success(function(ok) {
            if (!ok) {
                $scope.page.message = "This alias is already taken";
            } else {
                $scope.showAlias = false;
                if ($scope.profile) {
                    $location.path("/u/" + name + "/" + $scope.profile);
                } else {
                    $location.path("/u/" + name);
                }
            }
        }).error(function(err) {
            console.error(err);
            $scope.page.message = err;
        });
    };
}
