package com.ste.restaurant.controller;

import com.ste.restaurant.dto.StringDto;
import com.ste.restaurant.dto.TableTopDto;
import com.ste.restaurant.service.TableTopService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/rest/api/tables")
@RestController
public class TableTopController {

    @Autowired
    TableTopService tableTopService;

    // by admin
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public TableTopDto saveTable(@Valid @RequestBody TableTopDto tableTopDto) {
        return tableTopService.saveTable(tableTopDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/by-name")
    public TableTopDto getTableByName(@RequestParam String name) {
        return tableTopService.getTableByName(name);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{name}")
    public TableTopDto deleteTableByName(@PathVariable String name) {
        return tableTopService.deleteTableByName(name);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{name}")
    public TableTopDto updateTable(@PathVariable String name, @Valid @RequestBody TableTopDto tableTopDto) {
        return tableTopService.updateTable(name, tableTopDto);
    }

    // admin and waiter
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    @GetMapping
    public List<TableTopDto> getAllTables() {
        return tableTopService.getAllTables();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    @PatchMapping(path = "/{name}/status")
    public TableTopDto updateTableStatus(@PathVariable String name, @Valid @RequestBody StringDto status) {
        return tableTopService.updateTableStatusByName(name, status);
    }

    // public
    @PreAuthorize("permitAll()")
    @GetMapping(path = "/available")
    public List<TableTopDto> getAvailableTables() {
        return tableTopService.getAvailableTables();
    }


}