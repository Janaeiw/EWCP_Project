package com.wecorp.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wecorp.common.result.R;
import com.wecorp.entity.Dict;
import com.wecorp.entity.DictData;
import com.wecorp.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/dict")
@RequiredArgsConstructor
public class DictController {

    private final SystemService systemService;

    @GetMapping("/page")
    public R<Page<Dict>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String dictName,
            @RequestParam(required = false) Integer status
    ) {
        return R.ok(systemService.getDictPage(pageNum, pageSize, dictName, status));
    }

    @PostMapping
    public R<Void> create(@RequestBody Dict dict) {
        systemService.createDict(dict);
        return R.ok();
    }

    @PutMapping
    public R<Void> update(@RequestBody Dict dict) {
        systemService.updateDict(dict);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        systemService.deleteDict(id);
        return R.ok();
    }

    @GetMapping("/data/page")
    public R<Page<DictData>> dataPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String dictCode,
            @RequestParam(required = false) Integer status
    ) {
        return R.ok(systemService.getDictDataPage(pageNum, pageSize, dictCode, status));
    }

    @PostMapping("/data")
    public R<Void> createData(@RequestBody DictData dictData) {
        systemService.createDictData(dictData);
        return R.ok();
    }

    @PutMapping("/data")
    public R<Void> updateData(@RequestBody DictData dictData) {
        systemService.updateDictData(dictData);
        return R.ok();
    }

    @DeleteMapping("/data/{id}")
    public R<Void> deleteData(@PathVariable Long id) {
        systemService.deleteDictData(id);
        return R.ok();
    }

    @GetMapping("/all")
    public R<Map<String, List<DictData>>> all() {
        return R.ok(systemService.getAllDictData());
    }
}
