// 模拟后端动态生成路由
import { defineFakeRoute } from "vite-plugin-fake-server/client";

/**
 * roles：页面级别权限，这里模拟二种 "admin"、"common"
 * admin：管理员角色
 * common：普通角色
 */
const permissionRouter = {
  path: "/permission",
  meta: {
    title: "权限管理",
    icon: "ep:lollipop",
    rank: 10
  },
  children: [
    {
      path: "/permission/page/index",
      name: "PermissionPage",
      meta: {
        title: "页面权限",
        roles: ["admin", "common"]
      }
    },
    {
      path: "/permission/button",
      meta: {
        title: "按钮权限",
        roles: ["admin", "common"]
      },
      children: [
        {
          path: "/permission/button/router",
          component: "permission/button/index",
          name: "PermissionButtonRouter",
          meta: {
            title: "路由返回按钮权限",
            auths: [
              "permission:btn:add",
              "permission:btn:edit",
              "permission:btn:delete"
            ]
          }
        },
        {
          path: "/permission/button/login",
          component: "permission/button/perms",
          name: "PermissionButtonLogin",
          meta: {
            title: "登录接口返回按钮权限"
          }
        }
      ]
    }
  ]
};

/** 系统管理 */
const systemRouter = {
  path: "/system",
  meta: {
    title: "系统管理",
    icon: "ep:setting",
    rank: 99
    // roles: ["admin"]
  },
  children: [
    {
      path: "/system/user",
      component: "system/user/index",
      name: "SystemUser",
      meta: {
        title: "用户管理",
        roles: ["admin"]
      }
    },
    {
      path: "/system/role",
      component: "system/role/index",
      name: "SystemRole",
      meta: {
        title: "角色管理"
      }
    },
    {
      path: "/system/permission",
      component: "system/permission/index",
      name: "SystemPermission",
      meta: {
        title: "权限管理"
      }
    },
    {
      path: "/system/organization",
      component: "system/organization/index",
      name: "SystemOrganization",
      meta: {
        title: "机构管理"
      }
    },
    {
      path: "/system/menu",
      component: "system/menu/index",
      name: "SystemMenu",
      meta: {
        title: "菜单管理"
      }
    },
    {
      path: "/system/log",
      component: "system/log/index",
      name: "SystemLog",
      meta: {
        title: "日志管理"
      }
    },
    {
      path: "/system/dict",
      component: "system/dict/index",
      name: "SystemDict",
      meta: {
        title: "字典管理"
      }
    },
    {
      path: "/system/config",
      component: "system/config/index",
      name: "SystemConfig",
      meta: {
        title: "系统参数管理"
      }
    },
    {
      path: "/system/job",
      component: "system/job/index",
      name: "SystemJob",
      meta: {
        title: "定时任务管理"
      }
    }
  ]
};

/** 六库管理 */
const libraryRouter = {
  path: "/library",
  meta: {
    title: "六库管理",
    icon: "ep:collection",
    rank: 12
  },
  children: [
    {
      path: "/library/product",
      component: "library/product/index",
      name: "LibraryProduct",
      meta: {
        title: "产品库"
      }
    },
    {
      path: "/library/poster",
      component: "library/poster/index",
      name: "LibraryPoster",
      meta: {
        title: "海报库"
      }
    },
    {
      path: "/library/content",
      component: "library/content/index",
      name: "LibraryContent",
      meta: {
        title: "内容库"
      }
    },
    {
      path: "/library/script",
      component: "library/script/index",
      name: "LibraryScript",
      meta: {
        title: "话术库"
      }
    },
    {
      path: "/library/activity",
      component: "library/activity/index",
      name: "LibraryActivity",
      meta: {
        title: "活动库"
      }
    },
    {
      path: "/library/tool",
      component: "library/tool/index",
      name: "LibraryTool",
      meta: {
        title: "工具库"
      }
    }
  ]
};

export default defineFakeRoute([
  {
    url: "/get-async-routes",
    method: "get",
    response: () => {
      return {
        success: true,
        data: [permissionRouter, systemRouter, libraryRouter]
      };
    }
  }
]);
