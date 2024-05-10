package com.nf.neoflow.controller;

import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api("泛用查询")
@RestController
@AllArgsConstructor
@RequestMapping("${neo.prefix:/neo}/query")
public class QueryController {



}
