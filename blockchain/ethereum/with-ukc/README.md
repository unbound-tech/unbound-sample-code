# Ethereum with UKC Demo

This sample shows how to sign Etherium transactions using Unbound UKC. It is a JavaScript terminal application.

## Overview
This demo does the following:

* Create an Ethereum transaction

* Use UKC to sign that transaction

* Combine the signature and transaction and post them to the Ethereum network


## Usage
### Requirements
* A UKC server with EC key for signing
* An Infura Token (get it from [here](https://infura.io))
* NodeJS LTS

### Installation and configuration
* Install dependencies
    ```
    $ npm install
    ```
* Configure addresses and credentials in the *properties.file*
	
	
### Running the demo
```
$ node ethereum.js
```