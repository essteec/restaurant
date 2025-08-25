package com.ste.restaurant.service;

import com.ste.restaurant.dto.TableTopDto;
import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.entity.TableStatus;
import com.ste.restaurant.entity.TableTop;
import com.ste.restaurant.exception.*;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.TableTopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableTopServiceTest {

    @Mock
    private TableTopRepository tableRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private TableTopService tableTopService;

    private TableTop testTable;
    private TableTopDto testTableDto;
    private StringDto testStringDto;

    @BeforeEach
    void setUp() {
        // Test table entity - using a.java real object since it's an entity
        testTable = new TableTop();
        testTable.setTableId(1L);
        testTable.setTableNumber("T01");
        testTable.setCapacity(4);
        testTable.setTableStatus(TableStatus.AVAILABLE);

        // Test table DTO
        testTableDto = new TableTopDto();
        testTableDto.setTableNumber("T01");
        testTableDto.setCapacity(4);
        testTableDto.setTableStatus(TableStatus.AVAILABLE);

        // Test string DTO
        testStringDto = new StringDto();
        testStringDto.setName("OCCUPIED");

        org.springframework.test.util.ReflectionTestUtils.setField(tableTopService, "siteBaseUrl", "http://localhost:8080");
        org.springframework.test.util.ReflectionTestUtils.setField(tableTopService, "qrCodeDir", "qrcodes/");
    }

    @Test
    void saveTable_success() {
        // Arrange
        when(tableRepository.existsTableTopByTableNumber("T01")).thenReturn(false);
        doAnswer(invocation -> {
            TableTopDto dto = invocation.getArgument(0);
            TableTop entity = invocation.getArgument(1);
            entity.setTableNumber(dto.getTableNumber());
            entity.setCapacity(dto.getCapacity());
            entity.setTableStatus(dto.getTableStatus());
            return null;
        }).when(orderMapper).updateTableFromDto(any(TableTopDto.class), any(TableTop.class));
        when(tableRepository.save(any(TableTop.class))).thenReturn(testTable);
        when(orderMapper.tableTopToTableTopDto(testTable)).thenReturn(testTableDto);

        // Act
        TableTopDto result = tableTopService.saveTable(testTableDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTableNumber()).isEqualTo("T01");
        assertThat(result.getCapacity()).isEqualTo(4);
        assertThat(result.getTableStatus()).isEqualTo(TableStatus.AVAILABLE);
        verify(tableRepository).existsTableTopByTableNumber("T01");
        verify(orderMapper).updateTableFromDto(eq(testTableDto), any(TableTop.class));
        verify(tableRepository).save(any(TableTop.class));
        verify(orderMapper).tableTopToTableTopDto(testTable);
    }

    @Test
    void saveTable_nullTableNumber() {
        // Arrange
        testTableDto.setTableNumber(null);

        // Act & Assert
        assertThatThrownBy(() -> tableTopService.saveTable(testTableDto))
                .isInstanceOf(NullValueException.class);
        verify(tableRepository, never()).existsTableTopByTableNumber(any());
        verify(tableRepository, never()).save(any());
    }

    @Test
    void saveTable_tableAlreadyExists() {
        // Arrange
        when(tableRepository.existsTableTopByTableNumber("T01")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> tableTopService.saveTable(testTableDto))
                .isInstanceOf(AlreadyExistsException.class);
        verify(tableRepository).existsTableTopByTableNumber("T01");
        verify(tableRepository, never()).save(any());
    }

    @Test
    void getAllTables_success() {
        // Arrange
        List<TableTop> tables = Arrays.asList(testTable);
        List<TableTopDto> tableDtos = Arrays.asList(testTableDto);
        when(tableRepository.findAll()).thenReturn(tables);
        when(orderMapper.tableTopsToTableTopDtos(tables)).thenReturn(tableDtos);

        // Act
        List<TableTopDto> result = tableTopService.getAllTables();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTableNumber()).isEqualTo("T01");
        verify(tableRepository).findAll();
        verify(orderMapper).tableTopsToTableTopDtos(tables);
    }

    @Test
    void getAllTables_emptyResult() {
        // Arrange
        when(tableRepository.findAll()).thenReturn(Collections.emptyList());
        when(orderMapper.tableTopsToTableTopDtos(Collections.emptyList())).thenReturn(Collections.emptyList());

        // Act
        List<TableTopDto> result = tableTopService.getAllTables();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(tableRepository).findAll();
        verify(orderMapper).tableTopsToTableTopDtos(Collections.emptyList());
    }

    @Test
    void getTableByName_success() {
        // Arrange
        when(tableRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));
        when(orderMapper.tableTopToTableTopDto(testTable)).thenReturn(testTableDto);

        // Act
        TableTopDto result = tableTopService.getTableByName("T01");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTableNumber()).isEqualTo("T01");
        verify(tableRepository).findByTableNumber("T01");
        verify(orderMapper).tableTopToTableTopDto(testTable);
    }

    @Test
    void getTableByName_notFound() {
        // Arrange
        when(tableRepository.findByTableNumber("NonExistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tableTopService.getTableByName("NonExistent"))
                .isInstanceOf(NotFoundException.class);
        verify(tableRepository).findByTableNumber("NonExistent");
    }

    @Test
    void deleteTableByName_success() {
        // Arrange
        when(tableRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));
        when(orderMapper.tableTopToTableTopDto(testTable)).thenReturn(testTableDto);

        // Act
        TableTopDto result = tableTopService.deleteTableByName("T01");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTableNumber()).isEqualTo("T01");
        verify(tableRepository).findByTableNumber("T01");
        verify(tableRepository).delete(testTable);
        verify(orderMapper).tableTopToTableTopDto(testTable);
    }

    @Test
    void deleteTableByName_notFound() {
        // Arrange
        when(tableRepository.findByTableNumber("NonExistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tableTopService.deleteTableByName("NonExistent"))
                .isInstanceOf(NotFoundException.class);
        verify(tableRepository).findByTableNumber("NonExistent");
        verify(tableRepository, never()).delete(any());
    }

    @Test
    void updateTable_success() {
        // Arrange
        TableTopDto updateDto = new TableTopDto();
        updateDto.setTableNumber("T02");
        updateDto.setCapacity(6);

        when(tableRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));
        when(tableRepository.findByTableNumber("T02")).thenReturn(Optional.empty());
        when(tableRepository.save(testTable)).thenReturn(testTable);
        when(orderMapper.tableTopToTableTopDto(testTable)).thenReturn(testTableDto);

        // Act
        TableTopDto result = tableTopService.updateTable("T01", updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(tableRepository).findByTableNumber("T01");
        verify(tableRepository).findByTableNumber("T02");
        verify(tableRepository).save(testTable);
        verify(orderMapper).tableTopToTableTopDto(testTable);
        // Verify the table properties were updated
        assertThat(testTable.getTableNumber()).isEqualTo("T02");
        assertThat(testTable.getCapacity()).isEqualTo(6);
    }

    @Test
    void updateTable_tableNotFound() {
        // Arrange
        TableTopDto updateDto = new TableTopDto();
        updateDto.setTableNumber("T02");
        when(tableRepository.findByTableNumber("NonExistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tableTopService.updateTable("NonExistent", updateDto))
                .isInstanceOf(NotFoundException.class);
        verify(tableRepository).findByTableNumber("NonExistent");
        verify(tableRepository, never()).save(any());
    }

    @Test
    void updateTable_newNumberAlreadyExists() {
        // Arrange
        TableTopDto updateDto = new TableTopDto();
        updateDto.setTableNumber("T02");
        
        TableTop existingTable = new TableTop();
        existingTable.setTableNumber("T02");

        when(tableRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));
        when(tableRepository.findByTableNumber("T02")).thenReturn(Optional.of(existingTable));

        // Act & Assert
        assertThatThrownBy(() -> tableTopService.updateTable("T01", updateDto))
                .isInstanceOf(AlreadyExistsException.class);
        verify(tableRepository).findByTableNumber("T01");
        verify(tableRepository).findByTableNumber("T02");
        verify(tableRepository, never()).save(any());
    }

    @Test
    void updateTable_sameNumberNoChange() {
        // Arrange
        TableTopDto updateDto = new TableTopDto();
        updateDto.setTableNumber("T01");
        updateDto.setCapacity(6);

        when(tableRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));
        when(tableRepository.save(testTable)).thenReturn(testTable);
        when(orderMapper.tableTopToTableTopDto(testTable)).thenReturn(testTableDto);

        // Act
        TableTopDto result = tableTopService.updateTable("T01", updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(tableRepository).findByTableNumber("T01");
        // No second check for existing number since it's the same
        verify(tableRepository).save(testTable);
        // Should not change the number (stays T01), should update capacity
        assertThat(testTable.getTableNumber()).isEqualTo("T01");
        assertThat(testTable.getCapacity()).isEqualTo(6);
    }

    @Test
    void updateTable_nullTableNumber() {
        // Arrange
        TableTopDto updateDto = new TableTopDto();
        updateDto.setTableNumber(null);
        updateDto.setCapacity(6);

        when(tableRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));
        when(tableRepository.save(testTable)).thenReturn(testTable);
        when(orderMapper.tableTopToTableTopDto(testTable)).thenReturn(testTableDto);

        // Act
        TableTopDto result = tableTopService.updateTable("T01", updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(tableRepository).findByTableNumber("T01");
        verify(tableRepository).save(testTable);
        // Should not change the number (stays T01), should update capacity
        assertThat(testTable.getTableNumber()).isEqualTo("T01");
        assertThat(testTable.getCapacity()).isEqualTo(6);
    }

    @Test
    void updateTable_nullCapacity() {
        // Arrange
        TableTopDto updateDto = new TableTopDto();
        updateDto.setTableNumber("T02");
        updateDto.setCapacity(null);

        when(tableRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));
        when(tableRepository.findByTableNumber("T02")).thenReturn(Optional.empty());
        when(tableRepository.save(testTable)).thenReturn(testTable);
        when(orderMapper.tableTopToTableTopDto(testTable)).thenReturn(testTableDto);

        // Act
        TableTopDto result = tableTopService.updateTable("T01", updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(tableRepository).findByTableNumber("T01");
        verify(tableRepository).findByTableNumber("T02");
        verify(tableRepository).save(testTable);
        // Should change the number to T02, should not change capacity (stays 4)
        assertThat(testTable.getTableNumber()).isEqualTo("T02");
        assertThat(testTable.getCapacity()).isEqualTo(4);
    }

    @Test
    void updateTableStatusByName_success() {
        // Arrange
        when(tableRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));
        when(tableRepository.save(testTable)).thenReturn(testTable);
        when(orderMapper.tableTopToTableTopDto(testTable)).thenReturn(testTableDto);

        // Act
        TableTopDto result = tableTopService.updateTableStatusByName("T01", testStringDto);

        // Assert
        assertThat(result).isNotNull();
        verify(tableRepository).findByTableNumber("T01");
        verify(tableRepository).save(testTable);
        verify(orderMapper).tableTopToTableTopDto(testTable);
        assertThat(testTable.getTableStatus()).isEqualTo(TableStatus.OCCUPIED);
    }

    @Test
    void updateTableStatusByName_nullStatus() {
        // Arrange
        testStringDto.setName(null);

        // Act & Assert
        assertThatThrownBy(() -> tableTopService.updateTableStatusByName("T01", testStringDto))
                .isInstanceOf(NullValueException.class);
        verify(tableRepository, never()).findByTableNumber(any());
    }

    @Test
    void updateTableStatusByName_tableNotFound() {
        // Arrange
        when(tableRepository.findByTableNumber("NonExistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tableTopService.updateTableStatusByName("NonExistent", testStringDto))
                .isInstanceOf(NotFoundException.class);
        verify(tableRepository).findByTableNumber("NonExistent");
        verify(tableRepository, never()).save(any());
    }

    @Test
    void updateTableStatusByName_invalidStatus() {
        // Arrange
        testStringDto.setName("INVALID_STATUS");
        when(tableRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));

        // Act & Assert
        assertThatThrownBy(() -> tableTopService.updateTableStatusByName("T01", testStringDto))
                .isInstanceOf(InvalidValueException.class);
        verify(tableRepository).findByTableNumber("T01");
        verify(tableRepository, never()).save(any());
    }

    @Test
    void updateTableStatusByName_sameStatus() {
        // Arrange
        testStringDto.setName("AVAILABLE"); // Same as current status
        when(tableRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));

        // Act & Assert
        assertThatThrownBy(() -> tableTopService.updateTableStatusByName("T01", testStringDto))
                .isInstanceOf(AlreadyHasException.class);
        verify(tableRepository).findByTableNumber("T01");
        verify(tableRepository, never()).save(any());
    }

    @Test
    void updateTableStatusByName_caseInsensitive() {
        // Arrange
        testStringDto.setName("occupied"); // lowercase should work
        when(tableRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));
        when(tableRepository.save(testTable)).thenReturn(testTable);
        when(orderMapper.tableTopToTableTopDto(testTable)).thenReturn(testTableDto);

        // Act
        TableTopDto result = tableTopService.updateTableStatusByName("T01", testStringDto);

        // Assert
        assertThat(result).isNotNull();
        verify(tableRepository).findByTableNumber("T01");
        verify(tableRepository).save(testTable);
        assertThat(testTable.getTableStatus()).isEqualTo(TableStatus.OCCUPIED);
    }

    @Test
    void updateTableStatusByName_dirtyStatus() {
        // Arrange
        testStringDto.setName("DIRTY");
        when(tableRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));
        when(tableRepository.save(testTable)).thenReturn(testTable);
        when(orderMapper.tableTopToTableTopDto(testTable)).thenReturn(testTableDto);

        // Act
        TableTopDto result = tableTopService.updateTableStatusByName("T01", testStringDto);

        // Assert
        assertThat(result).isNotNull();
        verify(tableRepository).findByTableNumber("T01");
        verify(tableRepository).save(testTable);
        assertThat(testTable.getTableStatus()).isEqualTo(TableStatus.DIRTY);
    }

    @Test
    void getAvailableTables_success() {
        // Arrange
        List<TableTop> availableTables = Arrays.asList(testTable);
        List<TableTopDto> availableTableDtos = Arrays.asList(testTableDto);
        when(tableRepository.findAllByTableStatus(TableStatus.AVAILABLE)).thenReturn(availableTables);
        when(orderMapper.tableTopsToTableTopDtos(availableTables)).thenReturn(availableTableDtos);

        // Act
        List<TableTopDto> result = tableTopService.getAvailableTables();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTableNumber()).isEqualTo("T01");
        assertThat(result.get(0).getTableStatus()).isEqualTo(TableStatus.AVAILABLE);
        verify(tableRepository).findAllByTableStatus(TableStatus.AVAILABLE);
        verify(orderMapper).tableTopsToTableTopDtos(availableTables);
    }

    @Test
    void getAvailableTables_noAvailableTables() {
        // Arrange
        when(tableRepository.findAllByTableStatus(TableStatus.AVAILABLE)).thenReturn(Collections.emptyList());
        when(orderMapper.tableTopsToTableTopDtos(Collections.emptyList())).thenReturn(Collections.emptyList());

        // Act
        List<TableTopDto> result = tableTopService.getAvailableTables();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(tableRepository).findAllByTableStatus(TableStatus.AVAILABLE);
        verify(orderMapper).tableTopsToTableTopDtos(Collections.emptyList());
    }

    @Test
    void getAvailableTables_multipleAvailableTables() {
        // Arrange
        TableTop table2 = new TableTop();
        table2.setTableId(2L);
        table2.setTableNumber("T02");
        table2.setCapacity(6);
        table2.setTableStatus(TableStatus.AVAILABLE);

        TableTopDto tableDto2 = new TableTopDto();
        tableDto2.setTableNumber("T02");
        tableDto2.setCapacity(6);
        tableDto2.setTableStatus(TableStatus.AVAILABLE);

        List<TableTop> availableTables = Arrays.asList(testTable, table2);
        List<TableTopDto> availableTableDtos = Arrays.asList(testTableDto, tableDto2);
        
        when(tableRepository.findAllByTableStatus(TableStatus.AVAILABLE)).thenReturn(availableTables);
        when(orderMapper.tableTopsToTableTopDtos(availableTables)).thenReturn(availableTableDtos);

        // Act
        List<TableTopDto> result = tableTopService.getAvailableTables();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTableNumber()).isEqualTo("T01");
        assertThat(result.get(1).getTableNumber()).isEqualTo("T02");
        verify(tableRepository).findAllByTableStatus(TableStatus.AVAILABLE);
        verify(orderMapper).tableTopsToTableTopDtos(availableTables);
    }
}
