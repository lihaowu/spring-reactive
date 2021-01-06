package com.binecy.service;

import com.binecy.bean.Warehouse;
import org.springframework.stereotype.Service;

@Service
public class WarehouseService {
    public Warehouse mock(long id) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setName("天下第一仓");
        return warehouse;
    }
}
