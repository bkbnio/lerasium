# Lerasium

[![version](https://img.shields.io/maven-central/v/io.bkbn/lerasium-core?style=flat-square)](https://search.maven.org/search?q=io.bkbn%20lerasium-core)

## Table of Contents

- [What Is Lerasium](#what-is-lerasium)
- [Local Development](#local-development)
- [The Playground](#the-playground)

## What is Lerasium 

In a sentence, Lerasium is a highly opinionated service generator for Kotlin. It takes in a domain definition, and
generates
the boilerplate for your ORM, DAO, and API.

## Local Development

Lerasium should run locally right out of the box, no configuration necessary (assuming you have JDK 17+ installed).
New features can be built locally and published to your local maven repository with the `./gradlew publishToMavenLocal`
command!

## The Playground

This repo contains a `playground` module that contains a working example showcasing the power of Lerasium. Clone the
repo and give it a spin!
