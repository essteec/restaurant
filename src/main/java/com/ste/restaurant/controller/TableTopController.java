package com.ste.restaurant.controller;

import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.dto.TableTopDto;
import com.ste.restaurant.dto.TableTopDtoQr;
import com.ste.restaurant.service.TableTopService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/rest/api/tables")
@RestController
public class TableTopController {

    private final TableTopService tableTopService;

    public TableTopController(TableTopService tableTopService) {
        this.tableTopService = tableTopService;
    }

    // by admin
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public TableTopDto saveTable(@Valid @RequestBody TableTopDto tableTopDto) {
        return tableTopService.saveTable(tableTopDto);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
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
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/qr-codes")
    public ResponseEntity<Void> resetAllQrCodes() {  // return response ok
        tableTopService.createQrForTables();
        return ResponseEntity.ok().build();
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/qr-codes")
    public ResponseEntity<Void> deleteAllQrCodes() {
        tableTopService.deleteAllQrCodes();
        return ResponseEntity.ok().build();
    }

    // admin and waiter
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    @GetMapping(path = "/qr-codes")
    public List<TableTopDtoQr> getAllQrCodes() {
        return tableTopService.getAllQrCodes(); 
    }

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