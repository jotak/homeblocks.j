"use strict";
angular.module('homeblocks.editview', ['ngRoute'])
    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/u/:user/:profile/e', {
            templateUrl: 'editview.html',
            controller: 'editViewCtrl'
        });
    }])
    .controller("editViewCtrl", ['$scope', '$http', '$routeParams', '$rootScope', '$location', '$document', function ($scope, $http, $routeParams, $rootScope, $location, $document) {
        $rootScope.title = "homeblocks";
        $http.get('/api/user/' + $routeParams.user + "/profile/" + $routeParams.profile).success(function(ctx) {
            $rootScope.title = ctx.title;
            $scope.refUser = ctx.refUser;
            $scope.profile = ctx.profile;
            $scope.page = ctx.page;
            $scope.logged = ctx.logged;
            $scope.minPos = { x: 0, y: 0 };
            if ($scope.logged != $scope.refUser) {
                $location.path("u/" + $scope.refUser + "/" + $scope.profile);
            }
            fillPageStyle($scope.page.blocks, $scope.minPos);
            initEditListeners($scope, $location, $http, $document);
        }).error(function (data) {
            console.log('Error: ' + data);
        });
    }]);
function initEditListeners($scope, $location, $http, $document) {
    $scope.viewMode = function () {
        $location.path("u/" + $scope.refUser + "/" + $scope.profile);
    };
    $scope.onClickBlockTitle = function(block, focusId) {
        block.editTitle = true;
        setTimeout(function () {
            document.getElementById(focusId).focus();
        }, 30);
    };
    $scope.onEditItem = function(item, focusId) {
        item.editing = true;
        setTimeout(function () {
            document.getElementById(focusId).focus();
        }, 30);
    };
    $scope.onSaveItem = function(item) {
        item.editing = false;
        saveProfile($http, $scope);
    };
    $scope.onCreateLink = function(block) {
        var link = {
            title: "",
            url: "http://",
            description: "",
            editing: true
        };
        block.links.push(link);
        saveProfile($http, $scope);
    };
    $scope.onCreateListItem = function(block) {
        var item = {
            value: "",
            editing: true
        };
        block.list.push(item);
        saveProfile($http, $scope);
    };
    $scope.onDeleteItem = function(list, index) {
        list.splice(index, 1);
        saveProfile($http, $scope);
    };
    $scope.onItemUp = function(list, index) {
        var tmp = list[index - 1];
        list[index - 1] = list[index];
        list[index] = tmp;
        saveProfile($http, $scope);
    };
    $scope.onSaveBlock = function(block) {
        block.editTitle = false;
        saveProfile($http, $scope);
    };
    $scope.onCreateBlock = function(x, y, type) {
        var block = createEmptyBlock(x, y, type);
        if (block != null) {
            block.animate = true;
            $scope.page.blocks.push(block);
            fillPageStyle($scope.page.blocks, $scope.minPos);
            saveProfile($http, $scope);
        }
    };
    $scope.onSwapBlocks = function(b1, b2x, b2y) {
        var b2 = findBlockByPosition($scope.page.blocks, b2x, b2y);
        b2.posx = b1.posx;
        b2.posy = b1.posy;
        b1.posx = b2x;
        b1.posy = b2y;
        b1.animate = true;
        b2.animate = true;
        fillPageStyle($scope.page.blocks, $scope.minPos);
        saveProfile($http, $scope);
    };
    $scope.onDeleteBlock = function(block) {
        if (confirm("Delete this block?")) {
            var index = $scope.page.blocks.indexOf(block);
            if (index >= 0) {
                $scope.page.blocks.splice(index, 1);
                fillPageStyle($scope.page.blocks, $scope.minPos);
                saveProfile($http, $scope);
            }
        }
    };
}
function createEmptyBlock(x, y, type) {
    var block = new FrontBlock();
    block.posx = x;
    block.posy = y;
    block.type = type;
    if (type == "links" || type == "audio" || type == "video" || type == "image") {
        block.links = [];
    } else if (type == "list") {
        block.list = [];
    } else if (type == "note") {
        block.note = "";
    } else {
        console.log("Type " + type + " not implemented (yet?)");
        return null;
    }
    return block;
}
