package com.wenjunhuang.codeepiphany.hackerrank.model

import com.wenjunhuang.codeepiphany.model.Language.*

object demo {
  val DEMO_JULIA_TEMPLATE: ChallengeCodeTemplate = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    "\n\n",
    """#
      |# Complete the 'birthdayCakeCandles' function below.
      |#
      |# The function is expected to return an INTEGER.
      |# The function accepts INTEGER_ARRAY candles as parameter.
      |#
      |
      |function birthdayCakeCandles(candles)
      |
      |end
      |
      |
      |""".stripMargin,
    """fptr = open(ENV["OUTPUT_PATH"], "w")
      |
      |candles_count = parse(Int32, strip(readline(stdin)))
      |
      |candles = map(x -> parse(Int32, x), Array{String}(split(rstrip(readline(stdin)))))
      |
      |result = birthdayCakeCandles(candles)
      |
      |write(fptr, string(result) * "\n")
      |
      |close(fptr)
      |
      |""".stripMargin
  )

  val DEMO_JAVA_TEMPLATE: ChallengeCodeTemplate = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """import java.io.*;
      |import java.math.*;
      |import java.security.*;
      |import java.text.*;
      |import java.util.*;
      |import java.util.concurrent.*;
      |import java.util.function.*;
      |import java.util.regex.*;
      |import java.util.stream.*;
      |import static java.util.stream.Collectors.joining;
      |import static java.util.stream.Collectors.toList;
      |
      |
      |""".stripMargin,
    """class Result {
      |
      |    /*
      |     * Complete the 'birthdayCakeCandles' function below.
      |     *
      |     * The function is expected to return an INTEGER.
      |     * The function accepts INTEGER_ARRAY candles as parameter.
      |     */
      |
      |    public static int birthdayCakeCandles(List<Integer> candles) {
      |    // Write your code here
      |
      |    }
      |
      |}
      |""".stripMargin,
    """public class Solution {
      |    public static void main(String[] args) throws IOException {
      |        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
      |        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(System.getenv("OUTPUT_PATH")));
      |
      |        int candlesCount = Integer.parseInt(bufferedReader.readLine().trim());
      |
      |        List<Integer> candles = Stream.of(bufferedReader.readLine().replaceAll("\\s+$", "").split(" "))
      |            .map(Integer::parseInt)
      |            .collect(toList());
      |
      |        int result = Result.birthdayCakeCandles(candles);
      |
      |        bufferedWriter.write(String.valueOf(result));
      |        bufferedWriter.newLine();
      |
      |        bufferedReader.close();
      |        bufferedWriter.close();
      |    }
      |}
      |
      |""".stripMargin
  )

  val DEMO_R_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    "\n\n",
    """#
      |# Complete the 'birthdayCakeCandles' function below.
      |#
      |# The function is expected to return an INTEGER.
      |# The function accepts INTEGER_ARRAY candles as parameter.
      |#
      |
      |birthdayCakeCandles <- function(candles) {
      |    # Write your code here
      |
      |}
      |""".stripMargin.replace("\r\n", "\n"),
    """stdin <- file('stdin')
      |open(stdin)
      |
      |fptr <- file(Sys.getenv("OUTPUT_PATH"))
      |open(fptr, open = "w")
      |
      |candlesCount <- as.integer(trimws(readLines(stdin, n = 1, warn = FALSE), which = "both"))
      |candles <- strsplit(trimws(readLines(stdin, n = 1, warn = FALSE), which = "right"), " ")[[1]]
      |candles <- as.integer(candles)
      |
      |result <- birthdayCakeCandles(candles)
      |
      |writeLines(as.character(result), con = fptr)
      |
      |close(stdin)
      |close(fptr)
      |
      |""".stripMargin.replace("\r\n", "\n")
  )

  val DEMO_KOTLIN_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """import java.io.*
      |import java.math.*
      |import java.security.*
      |import java.text.*
      |import java.util.*
      |import java.util.concurrent.*
      |import java.util.function.*
      |import java.util.regex.*
      |import java.util.stream.*
      |import kotlin.collections.*
      |import kotlin.comparisons.*
      |import kotlin.io.*
      |import kotlin.jvm.*
      |import kotlin.jvm.functions.*
      |import kotlin.jvm.internal.*
      |import kotlin.ranges.*
      |import kotlin.sequences.*
      |import kotlin.text.*
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """/*
      | * Complete the 'birthdayCakeCandles' function below.
      | *
      | * The function is expected to return an INTEGER.
      | * The function accepts INTEGER_ARRAY candles as parameter.
      | */
      |
      |fun birthdayCakeCandles(candles: Array<Int>): Int {
      |    // Write your code here
      |
      |}
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """
      |fun main(args: Array<String>) {
      |    val candlesCount = readLine()!!.trim().toInt()
      |
      |    val candles = readLine()!!.trimEnd().split(" ").map{ it.toInt() }.toTypedArray()
      |
      |    val result = birthdayCakeCandles(candles)
      |
      |    println(result)
      |}
      |
      |""".stripMargin.replace("\r\n", "\n")
  )

  val DEMO_TYPESCRIPT_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """'use strict';
      |
      |import { WriteStream, createWriteStream } from "fs";
      |process.stdin.resume();
      |process.stdin.setEncoding('utf-8');
      |
      |let inputString: string = '';
      |let inputLines: string[] = [];
      |let currentLine: number = 0;
      |
      |process.stdin.on('data', function(inputStdin: string): void {
      |    inputString += inputStdin;
      |});
      |
      |process.stdin.on('end', function(): void {
      |    inputLines = inputString.split('\n');
      |    inputString = '';
      |
      |    main();
      |});
      |
      |function readLine(): string {
      |    return inputLines[currentLine++];
      |}
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """/*
      | * Complete the 'birthdayCakeCandles' function below.
      | *
      | * The function is expected to return an INTEGER.
      | * The function accepts INTEGER_ARRAY candles as parameter.
      | */
      |
      |function birthdayCakeCandles(candles: number[]): number {
      |    // Write your code here
      |
      |}
      |""".stripMargin.replace("\r\n", "\n"),
    """function main() {
      |    const ws: WriteStream = createWriteStream(process.env['OUTPUT_PATH']);
      |
      |    const candlesCount: number = parseInt(readLine().trim(), 10);
      |
      |    const candles: number[] = readLine().replace(/\s+$/g, '').split(' ').map(candlesTemp => parseInt(candlesTemp, 10));
      |
      |    const result: number = birthdayCakeCandles(candles);
      |
      |    ws.write(result + '\n');
      |
      |    ws.end();
      |}
      |""".stripMargin.replace("\r\n", "\n")
  )

  val DEMO_ERLANG_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """
      |-module(solution).
      |-export([main/0]).
      |-import(os, [getenv/1]).
      |
      |
      |""".stripMargin,
    """
      |%
      |% Complete the 'birthdayCakeCandles' function below.
      |%
      |% The function is expected to return an INTEGER.
      |% The function accepts INTEGER_ARRAY candles as parameter.
      |%
      |
      |birthdayCakeCandles(Candles) ->
      |    % Write your code here
      |
      |
      |""".stripMargin,
    """
      |main() ->
      |    {ok, Fptr} = file:open(getenv("OUTPUT_PATH"), [write]),
      |
      |    {CandlesCount, _} = string:to_integer(re:replace(io:get_line(""), "(^\\s+)|(\\s+$)", "", [global, {return, list}])),
      |
      |    CandlesTemp = re:split(re:replace(io:get_line(""), "\\s+$", "", [global, {return, list}]), "\\s+", [{return, list}]),
      |
      |    Candles = lists:map(fun(X) -> {I, _} = string:to_integer(re:replace(X, "(^\\s+)|(\\s+$)", "", [global, {return, list}])), I end, CandlesTemp),
      |
      |    Result = birthdayCakeCandles(Candles),
      |
      |    io:fwrite(Fptr, "~w~n", [Result]),
      |
      |    file:close(Fptr),
      |
      |    ok.
      |
      |""".stripMargin
  )

  val DEMO_CPP_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """
      |#include <bits/stdc++.h>
      |
      |using namespace std;
      |
      |string ltrim(const string &);
      |string rtrim(const string &);
      |vector<string> split(const string &);
      |
      |
      |""".stripMargin,
    """
      |/*
      | * Complete the 'birthdayCakeCandles' function below.
      | *
      | * The function is expected to return an INTEGER.
      | * The function accepts INTEGER_ARRAY candles as parameter.
      | */
      |
      |int birthdayCakeCandles(vector<int> candles) {
      |
      |}
      |
      |
      |""".stripMargin,
    """
      |int main()
      |{
      |    ofstream fout(getenv("OUTPUT_PATH"));
      |
      |    string candles_count_temp;
      |    getline(cin, candles_count_temp);
      |
      |    int candles_count = stoi(ltrim(rtrim(candles_count_temp)));
      |
      |    string candles_temp_temp;
      |    getline(cin, candles_temp_temp);
      |
      |    vector<string> candles_temp = split(rtrim(candles_temp_temp));
      |
      |    vector<int> candles(candles_count);
      |
      |    for (int i = 0; i < candles_count; i++) {
      |        int candles_item = stoi(candles_temp[i]);
      |
      |        candles[i] = candles_item;
      |    }
      |
      |    int result = birthdayCakeCandles(candles);
      |
      |    fout << result << "\n";
      |
      |    fout.close();
      |
      |    return 0;
      |}
      |
      |string ltrim(const string &str) {
      |    string s(str);
      |
      |    s.erase(
      |        s.begin(),
      |        find_if(s.begin(), s.end(), not1(ptr_fun<int, int>(isspace)))
      |    );
      |
      |    return s;
      |}
      |
      |string rtrim(const string &str) {
      |    string s(str);
      |
      |    s.erase(
      |        find_if(s.rbegin(), s.rend(), not1(ptr_fun<int, int>(isspace))).base(),
      |        s.end()
      |    );
      |
      |    return s;
      |}
      |
      |vector<string> split(const string &str) {
      |    vector<string> tokens;
      |
      |    string::size_type start = 0;
      |    string::size_type end = 0;
      |
      |    while ((end = str.find(" ", start)) != string::npos) {
      |        tokens.push_back(str.substr(start, end - start));
      |
      |        start = end + 1;
      |    }
      |
      |    tokens.push_back(str.substr(start));
      |
      |    return tokens;
      |}
      |
      |""".stripMargin
  )

  val DEMO_PHP_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """
      |<?php
      |
      |
      |""".stripMargin,
    """
      |/*
      | * Complete the 'birthdayCakeCandles' function below.
      | *
      | * The function is expected to return an INTEGER.
      | * The function accepts INTEGER_ARRAY candles as parameter.
      | */
      |
      |function birthdayCakeCandles($candles) {
      |    // Write your code here
      |
      |}
      |
      |
      |""".stripMargin,
    """
      |$fptr = fopen(getenv("OUTPUT_PATH"), "w");
      |
      |$candles_count = intval(trim(fgets(STDIN)));
      |
      |$candles_temp = rtrim(fgets(STDIN));
      |
      |$candles = array_map('intval', preg_split('/ /', $candles_temp, -1, PREG_SPLIT_NO_EMPTY));
      |
      |$result = birthdayCakeCandles($candles);
      |
      |fwrite($fptr, $result . "\n");
      |
      |fclose($fptr);
      |
      |""".stripMargin
  )

  val DEMO_JAVASCRIPT_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """'use strict';
      |
      |const fs = require('fs');
      |
      |process.stdin.resume();
      |process.stdin.setEncoding('utf-8');
      |
      |let inputString = '';
      |let currentLine = 0;
      |
      |process.stdin.on('data', function(inputStdin) {
      |    inputString += inputStdin;
      |});
      |
      |process.stdin.on('end', function() {
      |    inputString = inputString.split('\n');
      |
      |    main();
      |});
      |
      |function readLine() {
      |    return inputString[currentLine++];
      |}
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """/*
      | * Complete the 'birthdayCakeCandles' function below.
      | *
      | * The function is expected to return an INTEGER.
      | * The function accepts INTEGER_ARRAY candles as parameter.
      | */
      |
      |function birthdayCakeCandles(candles) {
      |    // Write your code here
      |
      |}
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """function main() {
      |    const ws = fs.createWriteStream(process.env.OUTPUT_PATH);
      |
      |    const candlesCount = parseInt(readLine().trim(), 10);
      |
      |    const candles = readLine().replace(/\s+$/g, '').split(' ').map(candlesTemp => parseInt(candlesTemp, 10));
      |
      |    const result = birthdayCakeCandles(candles);
      |
      |    ws.write(result + '\n');
      |
      |    ws.end();
      |}
      |
      |""".stripMargin.replace("\r\n", "\n")
  )

  val DEMO_SWIFT_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """import Foundation
      |""".stripMargin.replace("\r\n", "\n"),
    """/*
      | * Complete the 'birthdayCakeCandles' function below.
      | *
      | * The function is expected to return an INTEGER.
      | * The function accepts INTEGER_ARRAY candles as parameter.
      | */
      |
      |func birthdayCakeCandles(candles: [Int]) -> Int {
      |    // Write your code here
      |
      |}
      |""".stripMargin.replace("\r\n", "\n"),
    """let stdout = ProcessInfo.processInfo.environment["OUTPUT_PATH"]!
      |FileManager.default.createFile(atPath: stdout, contents: nil, attributes: nil)
      |let fileHandle = FileHandle(forWritingAtPath: stdout)!
      |
      |guard let candlesCount = Int((readLine()?.trimmingCharacters(in: .whitespacesAndNewlines))!)
      |else { fatalError("Bad input") }
      |
      |guard let candlesTemp = readLine()?.replacingOccurrences(of: "\\s+$", with: "", options: .regularExpression) else { fatalError("Bad input") }
      |
      |let candles: [Int] = candlesTemp.split(separator: " ").map {
      |    if let candlesItem = Int($0) {
      |        return candlesItem
      |    } else { fatalError("Bad input") }
      |}
      |
      |guard candles.count == candlesCount else { fatalError("Bad input") }
      |
      |let result = birthdayCakeCandles(candles: candles)
      |
      |fileHandle.write(String(result).data(using: .utf8)!)
      |fileHandle.write("\n".data(using: .utf8)!)
      |
      |""".stripMargin.replace("\r\n", "\n")
  )

  val DEMO_RUST_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """use std::env;
      |use std::fs::File;
      |use std::io::{self, BufRead, Write};
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """/*
      | * Complete the 'birthdayCakeCandles' function below.
      | *
      | * The function is expected to return an INTEGER.
      | * The function accepts INTEGER_ARRAY candles as parameter.
      | */
      |
      |fn birthdayCakeCandles(candles: &[i32]) -> i32 {
      |
      |}
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """fn main() {
      |    let stdin = io::stdin();
      |    let mut stdin_iterator = stdin.lock().lines();
      |
      |    let mut fptr = File::create(env::var("OUTPUT_PATH").unwrap()).unwrap();
      |
      |    let _candles_count = stdin_iterator.next().unwrap().unwrap().trim().parse::<i32>().unwrap();
      |
      |    let candles: Vec<i32> = stdin_iterator.next().unwrap().unwrap()
      |        .trim_end()
      |        .split(' ')
      |        .map(|s| s.to_string().parse::<i32>().unwrap())
      |        .collect();
      |
      |    let result = birthdayCakeCandles(&candles);
      |
      |    writeln!(&mut fptr, "{}", result).ok();
      |}
      |
      |""".stripMargin.replace("\r\n", "\n")
  )

  val DEMO_SCALA_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """import java.io._
      |import java.math._
      |import java.security._
      |import java.text._
      |import java.util._
      |import java.util.concurrent._
      |import java.util.function._
      |import java.util.regex._
      |import java.util.stream._
      |import scala.collection.immutable._
      |import scala.collection.mutable._
      |import scala.collection.concurrent._
      |import scala.concurrent._
      |import scala.io._
      |import scala.math._
      |import scala.sys._
      |import scala.util.matching._
      |import scala.reflect._
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """object Result {
      |
      |    /*
      |     * Complete the 'birthdayCakeCandles' function below.
      |     *
      |     * The function is expected to return an INTEGER.
      |     * The function accepts INTEGER_ARRAY candles as parameter.
      |     */
      |
      |    def birthdayCakeCandles(candles: Array[Int]): Int = {
      |    // Write your code here
      |
      |    }
      |
      |}
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """object Solution {
      |    def main(args: Array[String]) {
      |        val printWriter = new PrintWriter(sys.env("OUTPUT_PATH"))
      |
      |        val candlesCount = StdIn.readLine.trim.toInt
      |
      |        val candles = StdIn.readLine.replaceAll("\\s+$", "").split(" ").map(_.trim.toInt)
      |
      |        val result = Result.birthdayCakeCandles(candles)
      |
      |        printWriter.println(result)
      |
      |        printWriter.close()
      |    }
      |}
      |
      |""".stripMargin.replace("\r\n", "\n")
  )

  val DEMO_PERL_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """#!/usr/bin/perl
      |
      |use strict;
      |use warnings;
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """#
      |# Complete the 'birthdayCakeCandles' function below.
      |#
      |# The function is expected to return an INTEGER.
      |# The function accepts INTEGER_ARRAY candles as parameter.
      |#
      |
      |sub birthdayCakeCandles {
      |    # Write your code here
      |
      |}
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """open(my $fptr, '>', $ENV{'OUTPUT_PATH'});
      |
      |my $candles_count = ltrim(rtrim(my $candles_count_temp = <STDIN>));
      |
      |my $candles = rtrim(my $candles_temp = <STDIN>);
      |
      |my @candles = split /\s+/, $candles;
      |
      |my $result = birthdayCakeCandles \@candles;
      |
      |print $fptr "$result\n";
      |
      |close $fptr;
      |
      |sub ltrim {
      |    my $str = shift;
      |
      |    $str =~ s/^\s+//;
      |
      |    return $str;
      |}
      |
      |sub rtrim {
      |    my $str = shift;
      |
      |    $str =~ s/\s+$//;
      |
      |    return $str;
      |}
      |
      |""".stripMargin.replace("\r\n", "\n")
  )

  val DEMO_CSHARP_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """using System.CodeDom.Compiler;
      |using System.Collections.Generic;
      |using System.Collections;
      |using System.ComponentModel;
      |using System.Diagnostics.CodeAnalysis;
      |using System.Globalization;
      |using System.IO;
      |using System.Linq;
      |using System.Reflection;
      |using System.Runtime.Serialization;
      |using System.Text.RegularExpressions;
      |using System.Text;
      |using System;
      |
      |
      |""".stripMargin.replace("\r\n", "\r"),
    """class Result
      |{
      |
      |    /*
      |     * Complete the 'birthdayCakeCandles' function below.
      |     *
      |     * The function is expected to return an INTEGER.
      |     * The function accepts INTEGER_ARRAY candles as parameter.
      |     */
      |
      |    public static int birthdayCakeCandles(List<int> candles)
      |    {
      |
      |    }
      |
      |}
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """class Solution
      |{
      |    public static void Main(string[] args)
      |    {
      |        TextWriter textWriter = new StreamWriter(@System.Environment.GetEnvironmentVariable("OUTPUT_PATH"), true);
      |
      |        int candlesCount = Convert.ToInt32(Console.ReadLine().Trim());
      |
      |        List<int> candles = Console.ReadLine().TrimEnd().Split(' ').ToList().Select(candlesTemp => Convert.ToInt32(candlesTemp)).ToList();
      |
      |        int result = Result.birthdayCakeCandles(candles);
      |
      |        textWriter.WriteLine(result);
      |
      |        textWriter.Flush();
      |        textWriter.Close();
      |    }
      |}
      |
      |""".stripMargin.replace("\r\n", "\n")
  )

  val DEMO_HASKELL_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """{-# LANGUAGE DuplicateRecordFields, FlexibleInstances, UndecidableInstances #-}
      |
      |module Main where
      |
      |import Control.Monad
      |import Data.Array
      |import Data.Bits
      |import Data.List
      |import Data.List.Split
      |import Data.Set
      |import Data.Text
      |import Debug.Trace
      |import System.Environment
      |import System.IO
      |import System.IO.Unsafe
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """--
      |-- Complete the 'birthdayCakeCandles' function below.
      |--
      |-- The function is expected to return an INTEGER.
      |-- The function accepts INTEGER_ARRAY candles as parameter.
      |--
      |
      |birthdayCakeCandles candles = do
      |    -- Write your code here
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """lstrip = Data.Text.unpack . Data.Text.stripStart . Data.Text.pack
      |rstrip = Data.Text.unpack . Data.Text.stripEnd . Data.Text.pack
      |
      |main :: IO()
      |main = do
      |    stdout <- getEnv "OUTPUT_PATH"
      |    fptr <- openFile stdout WriteMode
      |
      |    candlesCountTemp <- getLine
      |    let candlesCount = read $ lstrip $ rstrip candlesCountTemp :: Int
      |
      |    candlesTemp <- getLine
      |
      |    let candles = Data.List.map (read :: String -> Int) . Data.List.words $ rstrip candlesTemp
      |
      |    let result = birthdayCakeCandles candles
      |
      |    hPutStrLn fptr $ show result
      |
      |    hFlush fptr
      |    hClose fptr
      |
      |""".stripMargin.replace("\r\n", "\n")
  )

  val DEMO_GO_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """package main
      |
      |import (
      |    "bufio"
      |    "fmt"
      |    "io"
      |    "os"
      |    "strconv"
      |    "strings"
      |)
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """/*
      | * Complete the 'birthdayCakeCandles' function below.
      | *
      | * The function is expected to return an INTEGER.
      | * The function accepts INTEGER_ARRAY candles as parameter.
      | */
      |
      |func birthdayCakeCandles(candles []int32) int32 {
      |    // Write your code here
      |
      |}
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """
      |func main() {
      |    reader := bufio.NewReaderSize(os.Stdin, 16 * 1024 * 1024)
      |
      |    stdout, err := os.Create(os.Getenv("OUTPUT_PATH"))
      |    checkError(err)
      |
      |    defer stdout.Close()
      |
      |    writer := bufio.NewWriterSize(stdout, 16 * 1024 * 1024)
      |
      |    candlesCount, err := strconv.ParseInt(strings.TrimSpace(readLine(reader)), 10, 64)
      |    checkError(err)
      |
      |    candlesTemp := strings.Split(strings.TrimSpace(readLine(reader)), " ")
      |
      |    var candles []int32
      |
      |    for i := 0; i < int(candlesCount); i++ {
      |        candlesItemTemp, err := strconv.ParseInt(candlesTemp[i], 10, 64)
      |        checkError(err)
      |        candlesItem := int32(candlesItemTemp)
      |        candles = append(candles, candlesItem)
      |    }
      |
      |    result := birthdayCakeCandles(candles)
      |
      |    fmt.Fprintf(writer, "%d\n", result)
      |
      |    writer.Flush()
      |}
      |
      |func readLine(reader *bufio.Reader) string {
      |    str, _, err := reader.ReadLine()
      |    if err == io.EOF {
      |        return ""
      |    }
      |
      |    return strings.TrimRight(string(str), "\r\n")
      |}
      |
      |func checkError(err error) {
      |    if err != nil {
      |        panic(err)
      |    }
      |}
      |
      |""".stripMargin.replace("\r\n", "\n")
  )

  val DEMO_RUBY_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """
      |#!/bin/ruby
      |
      |require 'json'
      |require 'stringio'
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """
      |#
      |# Complete the 'birthdayCakeCandles' function below.
      |#
      |# The function is expected to return an INTEGER.
      |# The function accepts INTEGER_ARRAY candles as parameter.
      |#
      |
      |def birthdayCakeCandles(candles)
      |    # Write your code here
      |
      |end
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """
      |fptr = File.open(ENV['OUTPUT_PATH'], 'w')
      |
      |candles_count = gets.strip.to_i
      |
      |candles = gets.rstrip.split.map(&:to_i)
      |
      |result = birthdayCakeCandles candles
      |
      |fptr.write result
      |fptr.write "\n"
      |
      |fptr.close()
      |
      |""".stripMargin.replace("\r\n", "\n")
  )

  val DEMO_CLOJURE_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    "\n\n",
    """
      |;
      |; Complete the 'birthdayCakeCandles' function below.
      |;
      |; The function is expected to return an INTEGER.
      |; The function accepts INTEGER_ARRAY candles as parameter.
      |;
      |
      |(defn birthdayCakeCandles [candles]
      |
      |)
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """
      |(def fptr (get (System/getenv) "OUTPUT_PATH"))
      |
      |(def candles-count (Integer/parseInt (clojure.string/trim (read-line))))
      |
      |(def candles (vec (map #(Integer/parseInt %) (clojure.string/split (clojure.string/trimr (read-line)) #" "))))
      |
      |(def result (birthdayCakeCandles candles))
      |
      |(spit fptr (str result "\n") :append true)
      |
      |""".stripMargin.replace("\r\n", "\n")
  )

  val DEMO_C_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """#include <assert.h>
      |#include <ctype.h>
      |#include <limits.h>
      |#include <math.h>
      |#include <stdbool.h>
      |#include <stddef.h>
      |#include <stdint.h>
      |#include <stdio.h>
      |#include <stdlib.h>
      |#include <string.h>
      |
      |char* readline();
      |char* ltrim(char*);
      |char* rtrim(char*);
      |char** split_string(char*);
      |
      |int parse_int(char*);
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """
      |/*
      | * Complete the 'birthdayCakeCandles' function below.
      | *
      | * The function is expected to return an INTEGER.
      | * The function accepts INTEGER_ARRAY candles as parameter.
      | */
      |
      |int birthdayCakeCandles(int candles_count, int* candles) {
      |
      |}
      |
      |
      |""".stripMargin,
    """
      |int main()
      |{
      |    FILE* fptr = fopen(getenv("OUTPUT_PATH"), "w");
      |
      |    int candles_count = parse_int(ltrim(rtrim(readline())));
      |
      |    char** candles_temp = split_string(rtrim(readline()));
      |
      |    int* candles = malloc(candles_count * sizeof(int));
      |
      |    for (int i = 0; i < candles_count; i++) {
      |        int candles_item = parse_int(*(candles_temp + i));
      |
      |        *(candles + i) = candles_item;
      |    }
      |
      |    int result = birthdayCakeCandles(candles_count, candles);
      |
      |    fprintf(fptr, "%d\n", result);
      |
      |    fclose(fptr);
      |
      |    return 0;
      |}
      |
      |char* readline() {
      |    size_t alloc_length = 1024;
      |    size_t data_length = 0;
      |
      |    char* data = malloc(alloc_length);
      |
      |    while (true) {
      |        char* cursor = data + data_length;
      |        char* line = fgets(cursor, alloc_length - data_length, stdin);
      |
      |        if (!line) {
      |            break;
      |        }
      |
      |        data_length += strlen(cursor);
      |
      |        if (data_length < alloc_length - 1 || data[data_length - 1] == '\n') {
      |            break;
      |        }
      |
      |        alloc_length <<= 1;
      |
      |        data = realloc(data, alloc_length);
      |
      |        if (!data) {
      |            data = '\0';
      |
      |            break;
      |        }
      |    }
      |
      |    if (data[data_length - 1] == '\n') {
      |        data[data_length - 1] = '\0';
      |
      |        data = realloc(data, data_length);
      |
      |        if (!data) {
      |            data = '\0';
      |        }
      |    } else {
      |        data = realloc(data, data_length + 1);
      |
      |        if (!data) {
      |            data = '\0';
      |        } else {
      |            data[data_length] = '\0';
      |        }
      |    }
      |
      |    return data;
      |}
      |
      |char* ltrim(char* str) {
      |    if (!str) {
      |        return '\0';
      |    }
      |
      |    if (!*str) {
      |        return str;
      |    }
      |
      |    while (*str != '\0' && isspace(*str)) {
      |        str++;
      |    }
      |
      |    return str;
      |}
      |
      |char* rtrim(char* str) {
      |    if (!str) {
      |        return '\0';
      |    }
      |
      |    if (!*str) {
      |        return str;
      |    }
      |
      |    char* end = str + strlen(str) - 1;
      |
      |    while (end >= str && isspace(*end)) {
      |        end--;
      |    }
      |
      |    *(end + 1) = '\0';
      |
      |    return str;
      |}
      |
      |char** split_string(char* str) {
      |    char** splits = NULL;
      |    char* token = strtok(str, " ");
      |
      |    int spaces = 0;
      |
      |    while (token) {
      |        splits = realloc(splits, sizeof(char*) * ++spaces);
      |
      |        if (!splits) {
      |            return splits;
      |        }
      |
      |        splits[spaces - 1] = token;
      |
      |        token = strtok(NULL, " ");
      |    }
      |
      |    return splits;
      |}
      |
      |int parse_int(char* str) {
      |    char* endptr;
      |    int value = strtol(str, &endptr, 10);
      |
      |    if (endptr == str || *endptr != '\0') {
      |        exit(EXIT_FAILURE);
      |    }
      |
      |    return value;
      |}
      |
      |""".stripMargin
  )

  val DEMO_OBJECTIVEC_TEMPLATE = ChallengeCodeTemplate(
    23074,
    "Birthday Cake Candles",
    "birthday-cake-candles",
    "Determine the number of candles that are blown out.",
    """
      |#import <Foundation/Foundation.h>
      |#import <objc/Object.h>
      |#import <objc/objc.h>
      |
      |@interface NSString (StringByTrimmingTrailingCharactersInSet)
      |- (NSString *) stringByTrimmingTrailingCharactersInSet:(NSCharacterSet *)characterSet;
      |@end
      |
      |@implementation NSString (StringByTrimmingTrailingCharactersInSet)
      |- (NSString *) stringByTrimmingTrailingCharactersInSet:(NSCharacterSet *)characterSet {
      |    NSRange end = [self rangeOfCharacterFromSet:[characterSet invertedSet] options:NSBackwardsSearch];
      |
      |    if (end.location == NSNotFound) {
      |        return @"";
      |    }
      |
      |    return [self substringToIndex:end.location + 1];
      |}
      |@end
      |
      |@interface NSString (NumberFromString)
      |- (NSNumber *) numberFromString:(NSNumberFormatter *)formatter;
      |@end
      |
      |@implementation NSString (NumberFromString)
      |- (NSNumber *) numberFromString:(NSNumberFormatter *)formatter {
      |    NSNumber *number = [formatter numberFromString:[self stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]]];
      |
      |    if (number == nil) {
      |        [NSException raise:@"Bad Input" format:@"%@", self];
      |    }
      |
      |    return number;
      |}
      |@end
      |
      |@interface NSString (ArrayFromString)
      |- (NSArray *) arrayFromString;
      |@end
      |
      |@implementation NSString (ArrayFromString)
      |- (NSArray *) arrayFromString {
      |    return [[self stringByTrimmingTrailingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]] componentsSeparatedByString:@" "];
      |}
      |@end
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """
      |@interface Solution:NSObject
      |- (NSNumber *) birthdayCakeCandles:(NSArray *)candles;
      |@end
      |
      |@implementation Solution
      |/*
      | * Complete the 'birthdayCakeCandles' function below.
      | *
      | * The function is expected to return an INTEGER.
      | * The function accepts INTEGER_ARRAY candles as parameter.
      | */
      |
      |- (NSNumber *) birthdayCakeCandles:(NSArray *)candles {
      |    // Write your code here
      |
      |}
      |
      |@end
      |
      |
      |""".stripMargin.replace("\r\n", "\n"),
    """
      |int main(int argc, const char* argv[]) {
      |    @autoreleasepool {
      |        NSString *stdout = [[[NSProcessInfo processInfo] environment] objectForKey:@"OUTPUT_PATH"];
      |        [[NSFileManager defaultManager] createFileAtPath:stdout contents:nil attributes:nil];
      |        NSFileHandle *fileHandle = [NSFileHandle fileHandleForWritingAtPath:stdout];
      |
      |        NSNumberFormatter *numberFormatter = [[NSNumberFormatter alloc] init];
      |
      |        NSData *availableInputData = [[NSFileHandle fileHandleWithStandardInput] availableData];
      |        NSString *availableInputString = [[NSString alloc] initWithData:availableInputData encoding:NSUTF8StringEncoding];
      |        NSArray *availableInputArray = [availableInputString componentsSeparatedByString:@"\n"];
      |
      |        NSUInteger currentInputLine = 0;
      |
      |        NSUInteger candlesCount = [[[[availableInputArray objectAtIndex:currentInputLine] stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]] numberFromString:numberFormatter] integerValue];
      |        currentInputLine += 1;
      |
      |        NSArray *candlesTemp = [[[availableInputArray objectAtIndex:currentInputLine] stringByTrimmingTrailingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]] componentsSeparatedByString:@" "];
      |        currentInputLine += 1;
      |
      |        NSMutableArray *candlesTempMutable = [NSMutableArray arrayWithCapacity:candlesCount];
      |
      |        for (NSString *candlesItem in candlesTemp) {
      |            [candlesTempMutable addObject:[candlesItem numberFromString:numberFormatter]];
      |        }
      |
      |        NSArray *candles = [candlesTempMutable copy];
      |
      |        NSNumber *result = [[[Solution alloc] init] birthdayCakeCandles:candles];
      |
      |        [fileHandle writeData:[[result stringValue] dataUsingEncoding:NSUTF8StringEncoding]];
      |        [fileHandle writeData:[@"\n" dataUsingEncoding:NSUTF8StringEncoding]];
      |
      |        [fileHandle closeFile];
      |    }
      |
      |    return 0;
      |}
      |
      |""".stripMargin.replace("\r\n", "\n")
  )

  val DEMOS = Map(
    Julia      -> DEMO_JULIA_TEMPLATE,
    Java       -> DEMO_JAVA_TEMPLATE,
    Javascript -> DEMO_JAVASCRIPT_TEMPLATE,
    R          -> DEMO_R_TEMPLATE,
    Kotlin     -> DEMO_KOTLIN_TEMPLATE,
    Typescript -> DEMO_TYPESCRIPT_TEMPLATE,
    ERLANG     -> DEMO_ERLANG_TEMPLATE,
    Cpp        -> DEMO_CPP_TEMPLATE,
    PHP        -> DEMO_PHP_TEMPLATE,
    Swift      -> DEMO_SWIFT_TEMPLATE,
    Rust       -> DEMO_RUST_TEMPLATE,
    Scala      -> DEMO_SCALA_TEMPLATE,
    Perl       -> DEMO_PERL_TEMPLATE,
    CSharp     -> DEMO_CSHARP_TEMPLATE,
    Haskell    -> DEMO_HASKELL_TEMPLATE,
    GO         -> DEMO_GO_TEMPLATE,
    Ruby       -> DEMO_RUBY_TEMPLATE,
    Clojure    -> DEMO_CLOJURE_TEMPLATE,
    C          -> DEMO_C_TEMPLATE,
    ObjectiveC -> DEMO_OBJECTIVEC_TEMPLATE
  )
}
