package com.tongxin.caihong.bean.company;

import com.tongxin.caihong.util.Constants;

import java.util.List;

public class StructBeanNetInfo {
    /**
     * companyName : 扶风
     * createTime : 1519964750
     * createUserId : 10008297
     * deleteTime : 0
     * deleteUserId : 0
     * empNum : 8
     * id : 5a98d24e4adfdc63ca4530c1
     * noticeContent : 暂无公告
     * noticeTime : 1519975095
     * rootDpartId : ["5a98d24e4adfdc63ca4530c2"]
     * type : 0
     */
    private String companyName;
    private int createTime;
    private int createUserId;
    private int deleteTime;
    private int deleteUserId;
    private int empNum;
    private String id;
    private String noticeContent;
    private int noticeTime;
    private int type;
    private List<DepartmentsBean> departments;
    private List<String> rootDpartId;
    // 公司是否通过审核 0 否 1是
    private int isChecked;
    // 邀请加入公司是否需要用户确认 0 否 1 是
    private int inviteJoinCompanyISNeedUserConfirm;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }

    public int getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(int createUserId) {
        this.createUserId = createUserId;
    }

    public int getDeleteTime() {
        return deleteTime;
    }

    public void setDeleteTime(int deleteTime) {
        this.deleteTime = deleteTime;
    }

    public int getDeleteUserId() {
        return deleteUserId;
    }

    public void setDeleteUserId(int deleteUserId) {
        this.deleteUserId = deleteUserId;
    }

    public int getEmpNum() {
        return empNum;
    }

    public void setEmpNum(int empNum) {
        this.empNum = empNum;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNoticeContent() {
        return noticeContent;
    }

    public void setNoticeContent(String noticeContent) {
        this.noticeContent = noticeContent;
    }

    public int getNoticeTime() {
        return noticeTime;
    }

    public void setNoticeTime(int noticeTime) {
        this.noticeTime = noticeTime;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<DepartmentsBean> getDepartments() {
        return departments;
    }

    public void setDepartments(List<DepartmentsBean> departments) {
        this.departments = departments;
    }

    public List<String> getRootDpartId() {
        return rootDpartId;
    }

    public void setRootDpartId(List<String> rootDpartId) {
        this.rootDpartId = rootDpartId;
    }

    public int getIsChecked() {
        return isChecked;
    }

    public void setIsChecked(int isChecked) {
        this.isChecked = isChecked;
    }

    public int getInviteJoinCompanyISNeedUserConfirm() {
        return inviteJoinCompanyISNeedUserConfirm;
    }

    public void setInviteJoinCompanyISNeedUserConfirm(int inviteJoinCompanyISNeedUserConfirm) {
        this.inviteJoinCompanyISNeedUserConfirm = inviteJoinCompanyISNeedUserConfirm;
    }

    public static class DepartmentsBean {
        /**
         * companyId : 5a98d24e4adfdc63ca4530c1
         * createTime : 1519964750
         * createUserId : 10008297
         * departName : 人事部
         * empNum : 8
         * id : 5a98d24e4adfdc63ca4530c3
         * parentId : 5a98d24e4adfdc63ca4530c2
         * type : 0
         */

        private String companyId;
        private int createTime;
        private int createUserId;
        private String departName;
        private int empNum;
        private String id;
        private String parentId;
        private int type;
        private List<EmployeesBean> employees;

        public String getCompanyId() {
            return companyId;
        }

        public void setCompanyId(String companyId) {
            this.companyId = companyId;
        }

        public int getCreateTime() {
            return createTime;
        }

        public void setCreateTime(int createTime) {
            this.createTime = createTime;
        }

        public int getCreateUserId() {
            return createUserId;
        }

        public void setCreateUserId(int createUserId) {
            this.createUserId = createUserId;
        }

        public String getDepartName() {
            return departName;
        }

        public void setDepartName(String departName) {
            this.departName = departName;
        }

        public int getEmpNum() {
            return empNum;
        }

        public void setEmpNum(int empNum) {
            this.empNum = empNum;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public List<EmployeesBean> getEmployees() {
            return employees;
        }

        public void setEmployees(List<EmployeesBean> employees) {
            this.employees = employees;
        }

        public static class EmployeesBean {
            /**
             * chatNum : 0
             * companyId : 5a98d24e4adfdc63ca4530c1
             * departmentId : 5a98d24e4adfdc63ca4530c3
             * id : 5a98d24e4adfdc63ca4530c6
             * isCustomer : 0
             * isPause : 0
             * operationType : 0
             * position : 员工
             * role : 3
             * userId : 10008297
             */

            private int chatNum;
            private String companyId;
            private String departmentId;
            private String id;
            private int isCustomer;
            private int isPause;
            private String nickname;
            private int operationType;
            private String position;
            private int role;
            private int userId;
            private int hiding;

            public int getChatNum() {
                return chatNum;
            }

            public void setChatNum(int chatNum) {
                this.chatNum = chatNum;
            }

            public String getCompanyId() {
                return companyId;
            }

            public void setCompanyId(String companyId) {
                this.companyId = companyId;
            }

            public String getDepartmentId() {
                return departmentId;
            }

            public void setDepartmentId(String departmentId) {
                this.departmentId = departmentId;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public int getIsCustomer() {
                return isCustomer;
            }

            public void setIsCustomer(int isCustomer) {
                this.isCustomer = isCustomer;
            }

            public int getIsPause() {
                return isPause;
            }

            public void setIsPause(int isPause) {
                this.isPause = isPause;
            }

            public String getNickname() {
                if (getHiding() == 1) {
                    return Constants.HIDING_NICKNAME;
                }
                return nickname;
            }

            public void setNickname(String nickname) {
                this.nickname = nickname;
            }

            public int getOperationType() {
                return operationType;
            }

            public void setOperationType(int operationType) {
                this.operationType = operationType;
            }

            public String getPosition() {
                return position;
            }

            public void setPosition(String position) {
                this.position = position;
            }

            public int getRole() {
                return role;
            }

            public void setRole(int role) {
                this.role = role;
            }

            public int getUserId() {
                return userId;
            }

            public void setUserId(int userId) {
                this.userId = userId;
            }

            public int getHiding() {
                return hiding;
            }

            public void setHiding(int hiding) {
                this.hiding = hiding;
            }
        }
    }
}
