# CoronaGame
Software Development Project 2020

What is this app? How does it do it? For who? For when? Why? 

CoronaGame was the initial name for a virus spreading game, supposed to help creating a model for virus spreading. Given the rapid evolution of the current coronavirus situation, we chose to adapt and created Virus Tracker. 

 

The main goal is to help control the spread by having more insight on it.  

It is made for everyone to use. 

The contamination analysis is mainly based on distance estimation between different users, and meeting occurrences with (potentially) infected users.  

We chose to make the app parametrizable with respect to the actual contamination factor and infection probability, as we can't pretend to know the actual numbers. 

 

Once you log into  the app, you can see 3 different tabs: 

Map: is a... map containing 5 feature buttons 

1) Toggle heatmap: show infected areas around you. If you zoom in/out too much, you won't be able to see it anymore. That way, you cannot see the precise location of infected users (privacy friendly). 

2) My location: when your fingers got lost on the map, click to see where you are on the map. 

3) Whole path: you are able to see the paths you made yesterday and the day before it, to have more insight on which infected areas you've been close too. This button allows you to show the whole path on the map, from starting point to ending point. NB: you need to have clicked on a path to show first (see 4 & 5). 

4) Yesterday: click here to see your path from yesterday. 

5) Before yesterday: click here to see your path from the day before yesterday. 

Status: concerns the user infection status, contains a graph showing your infection probability, and a button to report your infection. You are allowed to change your infection status at most once per day. 

Account: your user informations and the tab where you can sign out and clear your history. 

<!--Badges-->
[![Build Status](https://api.cirrus-ci.com/github/CoronaTeam/CoronaGame.svg)](https://cirrus-ci.com/github/CoronaTeam/CoronaGame)
[![Maintainability](https://api.codeclimate.com/v1/badges/2f13f697c44a03275527/maintainability)](https://codeclimate.com/github/CoronaTeam/CoronaGame/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/2f13f697c44a03275527/test_coverage)](https://codeclimate.com/github/CoronaTeam/CoronaGame/test_coverage)
