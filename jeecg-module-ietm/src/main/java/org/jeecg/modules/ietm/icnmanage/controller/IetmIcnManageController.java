package org.jeecg.modules.ietm.icnmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.ietm.icnmanage.entity.IetmIcnManage;
import org.jeecg.modules.ietm.icnmanage.service.IIetmIcnManageService;
import org.jeecg.modules.ietm.ietmattachment.entity.IetmAttachment;
import org.jeecg.modules.ietm.ietmattachment.service.impl.IetmAttachmentServiceImpl;
import org.jeecg.modules.ietm.projectconfigurationmanagement.entity.IetmProjectConfigurationManagement;
import org.jeecg.modules.ietm.projectconfigurationmanagement.service.IIetmProjectConfigurationManagementService;
import org.jeecg.modules.ietm.projectmanagement.entity.IetmProject;
import org.jeecg.modules.ietm.projectmanagement.service.IIetmProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @Description: 项目管理-项目实体管理
 * @Author: jeecg-boot
 * @Date: 2026-02-06
 * @Version: V1.0
 */
@Api(tags = "项目管理-项目实体管理")
@RestController
@RequestMapping("/icnmanage/ietmIcnManage")
@Slf4j
public class IetmIcnManageController extends JeecgController<IetmIcnManage, IIetmIcnManageService> {
    @Autowired
    private IIetmIcnManageService ietmIcnManageService;

    @Autowired
    private IetmAttachmentServiceImpl ietmAttachmentService;

    @Autowired
    private IIetmProjectConfigurationManagementService projectConfigurationManagementService;


    @Autowired
    private IIetmProjectService iIetmProjectService;



    /**
     * 分页列表查询
     *
     * @param ietmIcnManage
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    //@AutoLog(value = "项目管理-项目实体管理-分页列表查询")
    @ApiOperation(value = "项目管理-项目实体管理-分页列表查询", notes = "项目管理-项目实体管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<IetmIcnManage>> queryPageList(IetmIcnManage ietmIcnManage, @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo, @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize, HttpServletRequest req) {
        QueryWrapper<IetmIcnManage> queryWrapper = QueryGenerator.initQueryWrapper(ietmIcnManage, req.getParameterMap());

        Page<IetmIcnManage> page = new Page<IetmIcnManage>(pageNo, pageSize);
        IPage<IetmIcnManage> pageList = ietmIcnManageService.page(page, queryWrapper);
        //封装附件信息
        pageList.getRecords().forEach(ietm -> {
            QueryWrapper<IetmAttachment> ietmAttachmentQueryWrapper = new QueryWrapper<>();
            ietmAttachmentQueryWrapper.eq("pid", ietm.getId());
            ietmAttachmentQueryWrapper.eq("file_type", "实体文件");
            IetmAttachment one = ietmAttachmentService.getOne(ietmAttachmentQueryWrapper);
            ietm.setIetmAttachment(one);

            QueryWrapper<IetmAttachment> ietmAttachmentQueryWrapper2 = new QueryWrapper<>();
            ietmAttachmentQueryWrapper2.eq("pid", ietm.getId());
            ietmAttachmentQueryWrapper2.eq("file_type", "相关文件");
            IetmAttachment one2 = ietmAttachmentService.getOne(ietmAttachmentQueryWrapper2);
            ietm.setRelatedIetmAttachment(one2);
            String icn = "ICN";
            icn+=StringUtils.isNotBlank(ietm.getSns())?"-"+ietm.getSns():"";
            icn+=StringUtils.isNotBlank(ietm.getRpc())?"-"+ietm.getRpc():"";
            icn+=StringUtils.isNotBlank(ietm.getOriginator())?"-"+ietm.getOriginator():"";
            icn+=StringUtils.isNotBlank(ietm.getUniqueId())?"-"+ietm.getUniqueId():"";
            icn+=StringUtils.isNotBlank(ietm.getVariantCode())?"-"+ietm.getVariantCode():"";
            icn+=StringUtils.isNotBlank(ietm.getIssueNo())?"-"+ietm.getIssueNo():"";
            //安全级别不知道怎么变的先用密级
            icn+="-0"+ietm.getSecurity();
            ietm.setIcn(icn);
        });
        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param
     * @return
     */
    @ApiOperation(value = "项目管理-项目实体管理-添加", notes = "项目管理-项目实体管理-添加")
    //@RequiresPermissions("icnmanage:ietm_icn_manage:add")
    @PostMapping(value = "/fileAdd")
    public Result<String> fileAdd(@RequestParam(value = "files") MultipartFile[] files, String cmnodeId, Integer security, String uniqueId, String sns, String icnType, String variantCode, String issueNo, String originator, String originatorName, String rpc, String rpcName) throws IOException {

        ietmIcnManageService.fileAdd(files, cmnodeId, security, uniqueId, sns, icnType, variantCode, issueNo, originator, originatorName, rpc, rpcName);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param ietmIcnManage
     * @return
     */
    @AutoLog(value = "项目管理-项目实体管理-编辑")
    @ApiOperation(value = "项目管理-项目实体管理-编辑", notes = "项目管理-项目实体管理-编辑")
    //@RequiresPermissions("icnmanage:ietm_icn_manage:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody IetmIcnManage ietmIcnManage) {
        ietmIcnManageService.updateById(ietmIcnManage);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "项目管理-项目实体管理-通过id删除")
    @ApiOperation(value = "项目管理-项目实体管理-通过id删除", notes = "项目管理-项目实体管理-通过id删除")
    //@RequiresPermissions("icnmanage:ietm_icn_manage:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) String id) {
        ietmIcnManageService.removeAllByid(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "项目管理-项目实体管理-批量删除")
    @ApiOperation(value = "项目管理-项目实体管理-批量删除", notes = "项目管理-项目实体管理-批量删除")
    //@RequiresPermissions("icnmanage:ietm_icn_manage:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.ietmIcnManageService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    //@AutoLog(value = "项目管理-项目实体管理-通过id查询")
    @ApiOperation(value = "项目管理-项目实体管理-通过id查询", notes = "项目管理-项目实体管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<IetmIcnManage> queryById(@RequestParam(name = "id", required = true) String id) {
        IetmIcnManage ietmIcnManage = ietmIcnManageService.getById(id);
        if (ietmIcnManage == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(ietmIcnManage);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param ietmIcnManage
     */
    //@RequiresPermissions("icnmanage:ietm_icn_manage:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, IetmIcnManage ietmIcnManage) {
        return super.exportXls(request, ietmIcnManage, IetmIcnManage.class, "项目管理-项目实体管理");
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    //@RequiresPermissions("icnmanage:ietm_icn_manage:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, IetmIcnManage.class);
    }

    @GetMapping(value = "/getUniqueId")
    public Result<String> getUniqueId(@RequestParam(name = "cmnodeId", required = true) String cmnodeId) {
        //唯一识别码
        String uniqueId = "00001";
        QueryWrapper<IetmIcnManage> cmnodeId1 = new QueryWrapper<IetmIcnManage>().eq("cmnode_id", cmnodeId)
                .orderByDesc("unique_id");
        List<IetmIcnManage> list = ietmIcnManageService.list(cmnodeId1);
        if(list!=null && list.size()>0){
            Optional<IetmIcnManage> first = list.stream().findFirst();
            String uniqueId1 = first.get().getUniqueId();
            if(StringUtils.isNotBlank(uniqueId1)){
                try {
                    int number = Integer.parseInt(uniqueId1) + 1 ;
                    uniqueId = String.format("%05d", number);
                } catch (NumberFormatException e) {

                }
            }
        }
        return Result.OK(uniqueId);
    }

    @GetMapping(value = "/getProject")
    public Result<Map<String, Object>> getProject(@RequestParam(name = "cmnodeId", required = true) String cmnodeId) {
        HashMap<String, Object> result = new HashMap<>();
        //唯一识别码
        String uniqueId = "00001";
        //SNS
        String sns = "";

        QueryWrapper<IetmIcnManage> cmnodeId1 = new QueryWrapper<IetmIcnManage>().eq("cmnode_id", cmnodeId)
                .orderByDesc("unique_id");
        List<IetmIcnManage> list = ietmIcnManageService.list(cmnodeId1);
        if(list!=null && list.size()>0){
            Optional<IetmIcnManage> first = list.stream().findFirst();
            String uniqueId1 = first.get().getUniqueId();
            if(StringUtils.isNotBlank(uniqueId1)){
                try {
                    int number = Integer.parseInt(uniqueId1) + 1 ;
                    uniqueId = String.format("%05d", number);
                } catch (NumberFormatException e) {

                }
            }
        }
        IetmProjectConfigurationManagement ietmProjectConfigurationManagement = projectConfigurationManagementService.getById(cmnodeId);
        IetmProject ietmProject = iIetmProjectService.getById(ietmProjectConfigurationManagement.getProjectId());
        if (ietmProject == null) {
            return Result.error("未找到对应项目数据");
        }
        //获取当前编码规则
        ArrayList<String> thisRule = new ArrayList<>();
        thisRule.add(ietmProjectConfigurationManagement.getCode());
        //递归
        getThisCodeRule(ietmProjectConfigurationManagement.getPid(),thisRule);
        //获取项目编码规则
        String codeRule = ietmProject.getCodeRule();
        //处理编码规则获得SNS
        sns = getSNS(codeRule,thisRule);
        result.put("id", ietmProject.getId());
        result.put("security", ietmProject.getSecurity());
        result.put("uniqueId", uniqueId);
        result.put("sns", sns);
        return Result.OK(result);
    }

    private String getSNS(String codeRule, ArrayList<String> thisRule) {
        String SNS = "";
        String[] split = codeRule.split("-");
        //先反转
        Collections.reverse(thisRule);
        SNS+=thisRule.get(0)+"-" +thisRule.get(1)+"-";
        //取编码规则前五个
        for (int i = 1; i < split.length-2; i++) {
            try {
                String s = thisRule.get(i + 1);
                SNS+=s;
            }catch (IndexOutOfBoundsException e){
                SNS+=split[i];
            }
        }
        return SNS;
    }

    private void getThisCodeRule(String pid, ArrayList<String> strings) {
        IetmProjectConfigurationManagement byId = projectConfigurationManagementService.getById(pid);
        if(byId!=null){
            strings.add(byId.getCode());
            getThisCodeRule(byId.getPid(),strings);
        }
    }

    /**
     * 相关文件上传
     *
     * @param
     * @return
     */
    @ApiOperation(value = "项目管理-项目实体管理-相关文件上传", notes = "项目管理-项目实体管理-相关文件上传")
    @PostMapping(value = "/relatedFilesAdd")
    public Result<String> relatedFilesAdd(@RequestParam(value = "files") MultipartFile[] files, String id) throws IOException {
        ietmIcnManageService.relatedFilesAdd(files, id);
        return Result.OK("添加成功！");
    }


    /**
     * 差异上传
     *
     * @param
     * @return
     */
    @ApiOperation(value = "项目管理-项目实体管理-差异上传", notes = "项目管理-项目实体管理-差异上传")
    @PostMapping(value = "/diffFilesAdd")
    public Result<String> diffFilesAdd(@RequestParam(value = "files") MultipartFile[] files, String id, String uniqueId, String variantCode) throws IOException {
        ietmIcnManageService.diffFilesAdd(files, id,uniqueId,variantCode);
        return Result.OK("添加成功！");
    }

    /**
     * 差异上传
     *
     * @param
     * @return
     */
    @ApiOperation(value = "项目管理-项目实体管理-差异上传", notes = "项目管理-项目实体管理-差异上传")
    @PostMapping(value = "/newFilesAdd")
    public Result<String> newFilesAdd(@RequestParam(value = "files") MultipartFile[] files, String id, String uniqueId, String variantCode) throws IOException {
        ietmIcnManageService.diffFilesAdd(files, id,uniqueId,variantCode);
        return Result.OK("添加成功！");
    }
}
