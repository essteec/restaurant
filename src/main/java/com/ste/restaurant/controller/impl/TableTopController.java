package com.ste.restaurant.controller.impl;

import com.ste.restaurant.dto.TableTopDto;
import com.ste.restaurant.service.impl.TableTopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/rest/api/tabletop")
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class TableTopController {

    @Autowired
    TableTopService tableTopService;

    @PostMapping(path = "/save-table")
    public TableTopDto saveTable(@RequestBody TableTopDto tableTopDto) {
        return tableTopService.saveTable(tableTopDto);
    }

    @GetMapping(path = "/table-list")
    public List<TableTopDto> getAllTables() {
        return tableTopService.getAllTables();
    }

    @GetMapping(path = "/tables/by-name")
    public TableTopDto getTableByName(@RequestParam String name) {
        return tableTopService.getTableByName(name);
    }

    @DeleteMapping(path = "/tables/{name}")
    public TableTopDto deleteTableByName(@PathVariable String name) {
        return tableTopService.deleteTableByName(name);
    }

    @PutMapping(path = "/table-update/{name}")
    public TableTopDto updateTable(@PathVariable String name, @RequestBody TableTopDto tableTopDto) {
        return tableTopService.updateTable(name, tableTopDto);
    }

    @PatchMapping(path = "/tables/{name}/status")
    public TableTopDto updateTableStatus(@PathVariable String name, @RequestBody String role) {
        return tableTopService.updateTableStatusByName(name, role);
    }
}