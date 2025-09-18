import React, { useState } from 'react';
import { alpha } from '@mui/material/styles'
import { Box, Toolbar, Drawer, List, ListItemButton, ListItemText, Divider, Collapse, ListItemIcon } from '@mui/material';
import { Link } from 'react-router-dom';
import NavBar from './NavBar';

import {
    ExpandLess,
    ExpandMore,
    Dashboard,
    AddCircleOutline,
    ListAlt,
    People,
    StackedLineChart,
    Settings,
} from '@mui/icons-material';

const drawerWidth = 240;

interface LayoutProps {
    children: React.ReactNode;
}

const MenuItem: React.FC<{
    title: string;
    icon: React.ReactNode;
    menuKey: string;
    openMenu: string;
    handleMenuClick: (key: string) => void;
    items: { text: string; link: string; icon: React.ReactNode }[];
}> = ({ title, icon, menuKey, openMenu, handleMenuClick, items }) => (
    <>
        <ListItemButton onClick={() => handleMenuClick(menuKey)}>
            <ListItemIcon>{icon}</ListItemIcon>
            <ListItemText primary={title} />
            {openMenu === menuKey ? <ExpandLess /> : <ExpandMore />}
        </ListItemButton>
        <Collapse in={openMenu === menuKey} timeout="auto" unmountOnExit>
            <List component="div" disablePadding>
                {items.map((item, index) => (
                    <ListItemButton key={index} sx={{ pl: 4 }} component={Link} to={item.link}>
                        <ListItemIcon>{item.icon}</ListItemIcon>
                        <ListItemText primary={item.text} />
                    </ListItemButton>
                ))}
            </List>
        </Collapse>
    </>
);


const Layout: React.FC<LayoutProps> = ({ children }) => {
    const [openMenu, setOpenMenu] = useState('');

    const handleMenuClick = (menuKey: string) => {
        setOpenMenu(prevOpenMenu => (prevOpenMenu === menuKey ? '' : menuKey));
    };

    const menuItems = [
        {
            title: 'Operações',
            icon: <StackedLineChart color="success" />,
            menuKey: 'operacoes',
            items: [
                { text: 'Criar', link: '/operacoes/novo', icon: <AddCircleOutline color="primary" /> },
                { text: 'Visualizar', link: '/operacoes', icon: <ListAlt color="secondary" /> }
            ]
        },
        {
            title: 'Operadores',
            icon: <People color="info" />,
            menuKey: 'operadores',
            items: [
                { text: 'Criar', link: '/operadores/novo', icon: <AddCircleOutline color="primary" /> },
                { text: 'Visualizar', link: '/operadores', icon: <ListAlt color="secondary" /> }
            ]
        },
        {
            title: 'Estratégias',
            icon: <Settings color="warning" />,
            menuKey: 'estrategias',
            items: [
                { text: 'Criar', link: '/estrategias/novo', icon: <AddCircleOutline color="primary" /> },
                { text: 'Visualizar', link: '/estrategias', icon: <ListAlt color="secondary" /> }
            ]
        }
    ];

    return (
        <Box sx={{ display: 'flex' }}>
            <NavBar />
            <Drawer
                variant="permanent"
                sx={{
                    width: drawerWidth,
                    flexShrink: 0,
                    [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: 'border-box' },
                }}
            >
                <Toolbar />
                <Box sx={{ overflow: 'auto' }}>
                    <List>
                        <ListItemButton component={Link} to="/">
                            <ListItemIcon><Dashboard color="primary" /></ListItemIcon>
                            <ListItemText primary="Painel de Controle" />
                        </ListItemButton>

                        <Divider sx={{ my: 1 }} />

                        {menuItems.map((item) => (
                            <MenuItem
                                key={item.menuKey}
                                {...item}
                                openMenu={openMenu}
                                handleMenuClick={handleMenuClick}
                            />
                        ))}
                    </List>
                </Box>
            </Drawer>
            <Box
                component="main"
                sx={{
                    flexGrow: 1,
                    p: 2,
                    width: { sm: `calc(100% - ${drawerWidth}px)` },
                    background: (theme) => `linear-gradient(145deg, ${alpha(theme.palette.primary.main, 0.1)} 0%, ${theme.palette.background.default} 30%)`,
                    minHeight: '100vh',
                }}
            >
                <Toolbar />
                {children}
            </Box>
        </Box>
    );
};

export default Layout;